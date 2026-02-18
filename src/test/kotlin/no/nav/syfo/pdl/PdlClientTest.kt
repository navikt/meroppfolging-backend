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

class PdlClientTest :
    DescribeSpec(
        {
            val azureAdClient = mockk<AzureAdClient>()
            val pdlServer = WireMockServer(8080)
            listener(WireMockListener(pdlServer, ListenerMode.PER_TEST))
            val pdlClient = PdlClient(azureAdClient, "http://localhost:8080", "pdl.scope")

            beforeTest {
                every { azureAdClient.getSystemToken(any()) } returns "token"
            }

            describe("hentPersonstatus") {
                it("should return true if the user is younger than the given max age") {
                    pdlServer.stubHentPerson(yearsOld = 66)

                    val result = pdlClient.hentPersonstatus("12345678910", 67)
                    (result is PdlClient.PersonstatusResultat.Levende) shouldBe true
                    (result as PdlClient.PersonstatusResultat.Levende).erUnderMaksAlder shouldBe true
                }

                it("should return false if the user is max age or older") {
                    pdlServer.stubHentPerson(yearsOld = 67)

                    val result = pdlClient.hentPersonstatus("12345678910", 67)
                    (result is PdlClient.PersonstatusResultat.Levende) shouldBe true
                    (result as PdlClient.PersonstatusResultat.Levende).erUnderMaksAlder shouldBe false
                }

                it("should return true if the birth date is null") {
                    pdlServer.stubHentPerson(yearsOld = null)

                    val result = pdlClient.hentPersonstatus("12345678910", 67)
                    (result is PdlClient.PersonstatusResultat.Levende) shouldBe true
                    (result as PdlClient.PersonstatusResultat.Levende).erUnderMaksAlder shouldBe true
                }

                it("should return DOED if person has doedsfall") {
                    pdlServer.stubHentPerson(yearsOld = 55, deceased = true)

                    val result = pdlClient.hentPersonstatus("12345678910", 67)
                    (result is PdlClient.PersonstatusResultat.Doed) shouldBe true
                }

                it("should return LEVENDE if person has no doedsfall") {
                    pdlServer.stubHentPerson(yearsOld = 55, deceased = false)

                    val result = pdlClient.hentPersonstatus("12345678910", 67)
                    (result is PdlClient.PersonstatusResultat.Levende) shouldBe true
                }

                it("should return LEVENDE when foedselsdato list is empty") {
                    pdlServer.stubHentPerson(yearsOld = 55, foedselsdatoListeIsEmpty = true)

                    val result = pdlClient.hentPersonstatus("12345678910", 67)
                    (result is PdlClient.PersonstatusResultat.Levende) shouldBe true
                    (result as PdlClient.PersonstatusResultat.Levende).erUnderMaksAlder shouldBe true
                }

                it("should return UKJENT when pdl responds with error") {
                    pdlServer.stubHentPersonError()

                    val result = pdlClient.hentPersonstatus("12345678910", 67)
                    result shouldBe PdlClient.PersonstatusResultat.Ukjent
                }
            }
        },
    )

fun WireMockServer.stubHentPerson(yearsOld: Long?, deceased: Boolean = false, foedselsdatoListeIsEmpty: Boolean = false) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val foedselsdatoJson = yearsOld?.let { "\"${LocalDate.now().minusYears(it).format(formatter)}\"" } ?: "null"
    val foedselsdatoListJson =
        if (foedselsdatoListeIsEmpty) {
            "[]"
        } else {
            """[{ "foedselsdato": $foedselsdatoJson }]"""
        }
    // Exact date value is irrelevant in these tests, we only care that doedsfall is present.
    val doedsfallJson = if (deceased) """[{ "doedsdato": "2024-01-15" }]""" else "[]"
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
                                    "foedselsdato": $foedselsdatoListJson,
                                    "doedsfall": $doedsfallJson,
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

fun WireMockServer.stubHentPersonError() {
    this.stubFor(
        post(urlEqualTo("/"))
            .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON_VALUE))
            .willReturn(
                aResponse()
                    .withStatus(500)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
            ),
    )
}
