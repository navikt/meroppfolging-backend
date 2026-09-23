package no.nav.syfo.dokarkiv

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
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
