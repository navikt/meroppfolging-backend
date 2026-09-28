package no.nav.syfo.dokarkiv

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.JsonLogCapture
import no.nav.syfo.auth.azuread.AzureAdClient
import org.springframework.http.HttpStatus
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.jsonMapper
import java.util.UUID

const val JOURNALPOST_PATH = "/rest/journalpostapi/v1/journalpost"

class DokarkivClientTest :
    FunSpec(
        {
            val azureAdClient = mockk<AzureAdClient>()
            val dokarkivScope = "some-scope"
            val dokarkivServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort()).also {
                it.start()
            }
            val dokarkivUrl = "http://localhost:${dokarkivServer.port()}"

            val dokarkivClient = DokarkivClient(
                azureAdClient = azureAdClient,
                dokarkivUrl = dokarkivUrl,
                dokarkivScope = dokarkivScope,
            )

            beforeTest {
                dokarkivServer.resetAll()
                every { azureAdClient.getSystemToken(dokarkivScope) } returns UUID.randomUUID().toString()
            }
            afterSpec {
                dokarkivServer.resetAll()
                dokarkivServer.stop()
            }

            test("Returns DokarkivResponse when journalpost is created (201)") {
                dokarkivServer.stubJournalpostResponse(
                    journalpostId = "123",
                    journalstatus = "ENDELIG",
                    journalpostferdigstilt = true,
                    statusCode = HttpStatus.CREATED,
                )

                val response = dokarkivClient.postSingleDocumentToDokarkiv(
                    fnr = "12345678910",
                    pdf = ByteArray(0),
                    eksternReferanseId = UUID.randomUUID().toString(),
                    title = "tittel",
                    filnavn = "filnavn",
                    kanal = null,
                )

                response?.journalpostId shouldBe "123"
            }

            test("Returns DokarkivResponse when journalpost already exists and is ferdigstilt (409)") {
                dokarkivServer.stubJournalpostResponse(
                    journalpostId = "766811301",
                    journalstatus = "ENDELIG",
                    journalpostferdigstilt = true,
                    statusCode = HttpStatus.CONFLICT,
                )

                val response = dokarkivClient.postSingleDocumentToDokarkiv(
                    fnr = "12345678910",
                    pdf = ByteArray(0),
                    eksternReferanseId = UUID.randomUUID().toString(),
                    title = "tittel",
                    filnavn = "filnavn",
                    kanal = null,
                )

                response?.journalpostId shouldBe "766811301"
            }

            test("Returns null when journalpost conflict (409) is not ferdigstilt") {
                dokarkivServer.stubJournalpostResponse(
                    journalpostId = "766811301",
                    journalstatus = "MIDLERTIDIG",
                    journalpostferdigstilt = false,
                    statusCode = HttpStatus.CONFLICT,
                )

                val response = dokarkivClient.postSingleDocumentToDokarkiv(
                    fnr = "12345678910",
                    pdf = ByteArray(0),
                    eksternReferanseId = UUID.randomUUID().toString(),
                    title = "tittel",
                    filnavn = "filnavn",
                    kanal = null,
                )

                response.shouldBeNull()
            }

            test("Returns null on other client errors (400)") {
                dokarkivServer.stubJournalpostResponse(
                    journalpostId = "766811301",
                    journalstatus = "ENDELIG",
                    journalpostferdigstilt = true,
                    statusCode = HttpStatus.BAD_REQUEST,
                )

                val response = dokarkivClient.postSingleDocumentToDokarkiv(
                    fnr = "12345678910",
                    pdf = ByteArray(0),
                    eksternReferanseId = UUID.randomUUID().toString(),
                    title = "tittel",
                    filnavn = "filnavn",
                    kanal = null,
                )

                response.shouldBeNull()
            }

            listOf(400, 500, 409).forEach { status ->
                test("HTTP $status failure keeps status but omits private response and reference") {
                    val reference = UUID.randomUUID().toString()
                    val body = if (status == 409) {
                        """{"journalpostId":"PRIVATE_ID","journalpostferdigstilt":false,
                            "journalstatus":"MIDLERTIDIG","melding":"PRIVATE_BODY"}"""
                    } else {
                        "PRIVATE_BODY 12345678910"
                    }
                    dokarkivServer.stubFor(
                        WireMock.post(WireMock.urlPathEqualTo(JOURNALPOST_PATH)).willReturn(
                            aResponse().withStatus(
                                status
                            ).withBody(body).withHeader("Content-Type", "application/json"),
                        ),
                    )

                    JsonLogCapture(DokarkivClient::class.java).use { logs ->
                        dokarkivClient.postSingleDocumentToDokarkiv(
                            "12345678910",
                            byteArrayOf(1),
                            reference,
                            "PRIVATE_TITLE",
                            "document",
                            null,
                        ).shouldBeNull()
                        val event = logs.events().single()
                        event["upstream"].asText() shouldBe "dokarkiv"
                        event["operation"].asText() shouldBe "create_journalpost"
                        event["upstream_status"].asInt() shouldBe status
                        event["event_type"].asText() shouldBe if (status == 409) {
                            "dokarkiv_journalpost_not_finalized"
                        } else {
                            "dokarkiv_request_failed"
                        }
                        listOf(reference, "PRIVATE_BODY", "PRIVATE_ID", "PRIVATE_TITLE", "12345678910")
                            .forEach { logs.text() shouldNotContain it }
                    }
                }
            }

            test("Malformed conflict response is diagnosed once without changing retry outcome") {
                dokarkivServer.stubFor(
                    WireMock.post(WireMock.urlPathEqualTo(JOURNALPOST_PATH)).willReturn(
                        aResponse().withStatus(409).withBody("PRIVATE_BODY 12345678910")
                            .withHeader("Content-Type", "application/json"),
                    ),
                )
                JsonLogCapture(DokarkivClient::class.java).use { logs ->
                    dokarkivClient.postSingleDocumentToDokarkiv(
                        "12345678910",
                        byteArrayOf(1),
                        UUID.randomUUID().toString(),
                        "title",
                        "document",
                        null,
                    ).shouldBeNull()
                    val event = logs.events().single()
                    event["event_type"].asText() shouldBe "dokarkiv_conflict_response_invalid"
                    event["upstream_status"].asInt() shouldBe 409
                    event.has("exception_type") shouldBe true
                    event.has("stack_trace") shouldBe true
                    logs.text() shouldNotContain "PRIVATE_BODY"
                    logs.text() shouldNotContain "12345678910"
                }
            }
        },
    )

private val dokarkivTestObjectMapper = jsonMapper {
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
}

fun WireMockServer.stubJournalpostResponse(
    journalpostId: String,
    journalstatus: String,
    journalpostferdigstilt: Boolean,
    statusCode: HttpStatus,
) {
    val body = mapOf(
        "journalpostId" to journalpostId,
        "journalstatus" to journalstatus,
        "melding" to null,
        "journalpostferdigstilt" to journalpostferdigstilt,
        "dokumenter" to listOf(mapOf("dokumentInfoId" to 805507073)),
    )
    this.stubFor(
        WireMock.post(WireMock.urlPathEqualTo(JOURNALPOST_PATH)).willReturn(
            aResponse().withBody(dokarkivTestObjectMapper.writeValueAsString(body))
                .withHeader("Content-Type", "application/json").withStatus(statusCode.value()),
        ),
    )
}
