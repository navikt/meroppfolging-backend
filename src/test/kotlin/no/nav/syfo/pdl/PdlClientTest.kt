package no.nav.syfo.pdl

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.wiremock.ListenerMode
import io.kotest.extensions.wiremock.WireMockListener
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.auth.azuread.AzureAdClient
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PdlClientTest : DescribeSpec(
    {
        val azureAdClient = mockk<AzureAdClient>()
        val pdlServer = WireMockServer(8080)
        listener(WireMockListener(pdlServer, ListenerMode.PER_TEST))
        val pdlClient = PdlClient(azureAdClient, "http://localhost:8080", "pdl.scope")

        beforeTest {
            every { azureAdClient.getSystemToken(any()) } returns "token"
        }

        describe("isBrukerYngreEnnGittMaxAlder") {
            it("should return true if the user is younger than the given max age") {
                pdlServer.stubHentPerson(yearsOld = 66)

                pdlClient.isBrukerYngreEnnGittMaxAlder("12345678910", 67) shouldBe true
            }

            it("should return false if the user is max age or older") {
                pdlServer.stubHentPerson(yearsOld = 67)

                pdlClient.isBrukerYngreEnnGittMaxAlder("12345678910", 67) shouldBe false
            }

            it("should return true if the birth date is null") {
                pdlServer.stubHentPerson(yearsOld = null)

                pdlClient.isBrukerYngreEnnGittMaxAlder("12345678910", 67) shouldBe true
            }
        }
    },
)

fun WireMockServer.stubHentPerson(yearsOld: Long?) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val foedselsdatoJson = yearsOld?.let { "\"${LocalDate.now().minusYears(it).format(formatter)}\"" } ?: "null"
    this.stubFor(
        post(urlEqualTo("/"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON_VALUE))
            .willReturn(
                aResponse()
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(
                        """
                        {
                            "data": {
                                "hentPerson": {
                                    "foedselsdato": [
                                        { "foedselsdato": $foedselsdatoJson }
                                    ],
                                    "navn": [
                                        {
                                            "fornavn": "Ola",
                                            "mellomnavn": "Normann",
                                            "etternavn": "Nordmann"
                                        }
                                    ]
                                }
                            }
                        }
                        """.trimIndent(),
                    ),
            ),
    )
}
