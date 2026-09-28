package no.nav.syfo.kartlegging

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.JsonLogCapture
import no.nav.syfo.documentFailureFields
import no.nav.syfo.dokarkiv.DokarkivClient
import no.nav.syfo.dokarkiv.domain.DokarkivResponse
import no.nav.syfo.kartlegging.database.KartleggingssporsmalDAO
import no.nav.syfo.kartlegging.domain.PersistedKartleggingssporsmal
import no.nav.syfo.kartlegging.domain.formsnapshot.FormSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.TextFieldSnapshot
import no.nav.syfo.kartlegging.job.JournalforingJob
import no.nav.syfo.kartlegging.service.KartleggingssporsmalService
import no.nav.syfo.leaderelection.LeaderElectionClient
import no.nav.syfo.syfoopppdfgen.PdfgenClient
import no.nav.syfo.syfoopppdfgen.PdfgenService
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.sql.SQLException
import java.time.Instant
import java.util.UUID

class JournalforingDiagnosticsTest :
    FunSpec({
        // Accepted by runtime-error v1 and the dashboard's exception/cause fields.
        val exceptionCategory = Regex("^([A-Za-z][A-Za-z0-9_.:$]{0,143})?(Error|Exception)$")
        val answer = PersistedKartleggingssporsmal(
            uuid = UUID.randomUUID(),
            fnr = "12345678910",
            kandidatId = UUID.randomUUID(),
            formSnapshot = FormSnapshot(
                "kartlegging",
                "1.0",
                "1",
                listOf(TextFieldSnapshot("field", "Question", value = "PRIVATE_FORM_ANSWER")),
            ),
            createdAt = Instant.now(),
        )

        test("PDF transport failure is diagnosed once and does not stop the job or consume the retry") {
            val dao = mockk<KartleggingssporsmalDAO>(relaxed = true)
            val dokarkiv = mockk<DokarkivClient>()
            val restTemplate = mockk<RestTemplate>()
            val pdfClient = PdfgenClient("http://syfooppdfgen", restTemplate)
            val pdfService = PdfgenService(pdfClient, mockk())
            val service = KartleggingssporsmalService(dao, mockk(), pdfService, dokarkiv)
            val leader = mockk<LeaderElectionClient>()
            val job = JournalforingJob(leader, service)
            val nextAnswer = answer.copy(uuid = UUID.randomUUID())
            val failure = ResourceAccessException(
                "Request for ${answer.fnr} ${answer.uuid} PRIVATE_FORM_ANSWER PRIVATE_TOKEN failed",
                SocketException("Unexpected end of file from server"),
            )
            var attempts = 0
            every {
                restTemplate.exchange(any<String>(), HttpMethod.POST, any<HttpEntity<*>>(), ByteArray::class.java)
            } answers {
                if (attempts++ == 0) throw failure
                ResponseEntity(byteArrayOf(1, 2), HttpStatus.OK)
            }
            every { leader.isPodLeader() } returns true
            every { dao.getKartleggingssporsmalNotJournaled() } returns listOf(answer, nextAnswer) andThen
                listOf(answer)
            every {
                dokarkiv.postSingleDocumentToDokarkiv(any(), any(), any(), any(), any(), any())
            } returns DokarkivResponse(
                journalpostId = "journalpost",
                journalstatus = "ENDELIG",
                journalpostferdigstilt = true,
            )

            JsonLogCapture(KartleggingssporsmalService::class.java, PdfgenClient::class.java).use { logs ->
                job.journalforSvarForKartleggingssporsmal()
                verify(exactly = 0) { dao.setJournalpostIdForKartleggingssporsmal(answer.uuid, any()) }
                verify(exactly = 1) { dao.setJournalpostIdForKartleggingssporsmal(nextAnswer.uuid, "journalpost") }

                job.journalforSvarForKartleggingssporsmal()
                verify(exactly = 1) { dao.setJournalpostIdForKartleggingssporsmal(answer.uuid, "journalpost") }
                val event = logs.events().single()
                event["event_type"].asText() shouldBe "kartlegging_journalforing_failed"
                event["operation"].asText() shouldBe "generate_kartlegging_pdf"
                event["upstream"].asText() shouldBe "syfooppdfgen"
                event["exception_type"].asText() shouldBe ResourceAccessException::class.java.simpleName
                event["cause_type"].asText() shouldBe SocketException::class.java.simpleName
                exceptionCategory.matches(event["exception_type"].asText()) shouldBe true
                exceptionCategory.matches(event["cause_type"].asText()) shouldBe true
                event.has("upstream_status") shouldBe false
                event["stack_trace"].asText() shouldContain "Unexpected end of file from server"
                event["stack_trace"].asText() shouldContain "JournalforingDiagnosticsTest"
                listOf(
                    answer.fnr,
                    answer.uuid.toString(),
                    answer.kandidatId.toString(),
                    "PRIVATE_FORM_ANSWER",
                    "PRIVATE_TOKEN"
                )
                    .forEach { logs.text() shouldNotContain it }
            }
        }

        test("PDF HTTP failure preserves status and safe stack without exposing the response body") {
            val dao = mockk<KartleggingssporsmalDAO>(relaxed = true)
            val dokarkiv = mockk<DokarkivClient>()
            val restTemplate = mockk<RestTemplate>()
            val pdfClient = PdfgenClient("http://syfooppdfgen", restTemplate)
            val service = KartleggingssporsmalService(dao, mockk(), PdfgenService(pdfClient, mockk()), dokarkiv)
            every {
                restTemplate.exchange(any<String>(), HttpMethod.POST, any<HttpEntity<*>>(), ByteArray::class.java)
            } throws HttpServerErrorException.create(
                HttpStatus.BAD_GATEWAY,
                "PRIVATE_STATUS_TEXT",
                HttpHeaders.EMPTY,
                "PRIVATE_RESPONSE_BODY ${answer.fnr}".toByteArray(),
                Charsets.UTF_8,
            )

            JsonLogCapture(KartleggingssporsmalService::class.java, PdfgenClient::class.java).use { logs ->
                service.jornalforKartleggingssporsmal(answer)
                logs.events().map { it["event_type"].asText() } shouldBe listOf(
                    "pdf_generation_failed",
                    "kartlegging_journalforing_failed",
                )
                val event = logs.events().last()
                event["upstream_status"].asInt() shouldBe 502
                event["exception_type"].asText() shouldBe HttpServerErrorException::class.java.simpleName
                exceptionCategory.matches(event["exception_type"].asText()) shouldBe true
                event["stack_trace"].asText() shouldContain HttpServerErrorException.BadGateway::class.java.name
                listOf("PRIVATE_STATUS_TEXT", "PRIVATE_RESPONSE_BODY", answer.fnr, answer.uuid.toString())
                    .forEach { logs.text() shouldNotContain it }
                verify(exactly = 0) { dokarkiv.postSingleDocumentToDokarkiv(any(), any(), any(), any(), any(), any()) }
                verify(exactly = 0) { dao.setJournalpostIdForKartleggingssporsmal(any(), any()) }
            }
        }

        test("wrapped timeout keeps its native root cause without copying private exception messages") {
            val failure = ResourceAccessException(
                "PRIVATE_REQUEST",
                IOException("PRIVATE_WRAPPER", SocketTimeoutException("PRIVATE_SOCKET_MESSAGE")),
            )
            val fields = failure.documentFailureFields()

            fields["exception_type"] shouldBe "ResourceAccessException"
            fields["cause_type"] shouldBe "SocketTimeoutException"
            fields.containsKey("upstream_status") shouldBe false
            (fields["stack_trace"] as String) shouldContain SocketTimeoutException::class.java.name
            fields.toString() shouldNotContain "PRIVATE_"
        }

        test("database failure after journaling is not diagnosed as an upstream PDF failure") {
            val dao = mockk<KartleggingssporsmalDAO>()
            val dokarkiv = mockk<DokarkivClient>()
            val pdfService = mockk<PdfgenService>()
            val service = KartleggingssporsmalService(dao, mockk(), pdfService, dokarkiv)
            every { pdfService.getKartleggingsPdf(answer, answer.createdAt) } returns byteArrayOf(1)
            every {
                dokarkiv.postSingleDocumentToDokarkiv(any(), any(), any(), any(), any(), any())
            } returns DokarkivResponse(
                journalpostId = "journalpost",
                journalstatus = "ENDELIG",
                journalpostferdigstilt = true,
            )
            every { dao.setJournalpostIdForKartleggingssporsmal(any(), any()) } throws SQLException("PRIVATE_DB_DATA")

            JsonLogCapture(KartleggingssporsmalService::class.java).use { logs ->
                service.jornalforKartleggingssporsmal(answer)
                val event = logs.events().single()
                event["operation"].asText() shouldBe "store_journalpost_id"
                event["exception_type"].asText() shouldBe SQLException::class.java.simpleName
                event.has("upstream") shouldBe false
                logs.text() shouldNotContain "PRIVATE_DB_DATA"
            }
        }
    })
