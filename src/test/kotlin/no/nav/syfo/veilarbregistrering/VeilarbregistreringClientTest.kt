package no.nav.syfo.veilarbregistrering

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.wiremock.ListenerMode
import io.kotest.extensions.wiremock.WireMockListener
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.auth.tokendings.TokendingsClient
import no.nav.syfo.config.kafka.jacksonMapper
import no.nav.syfo.senoppfolging.v1.domain.AndreForholdSvar
import no.nav.syfo.senoppfolging.v1.domain.Besvarelse
import no.nav.syfo.senoppfolging.v1.domain.FremtidigSituasjonSvar
import no.nav.syfo.senoppfolging.v1.domain.SenOppfolgingRegistrering
import no.nav.syfo.senoppfolging.v1.domain.SisteStillingSvar
import no.nav.syfo.senoppfolging.v1.domain.TekstForSporsmal
import no.nav.syfo.senoppfolging.v1.domain.TilbakeIArbeidSvar
import no.nav.syfo.senoppfolging.v1.domain.UtdanningBestattSvar
import no.nav.syfo.senoppfolging.v1.domain.UtdanningGodkjentSvar
import no.nav.syfo.senoppfolging.v1.domain.UtdanningSvar
import no.nav.syfo.veilarbregistrering.domain.StartRegistrationDTO
import no.nav.syfo.veilarbregistrering.domain.StartRegistrationType
import org.springframework.http.HttpHeaders

class VeilarbregistreringClientTest : FunSpec(
    {
        val tokendingsClient: TokendingsClient = mockk<TokendingsClient>()
        val baseUrl = "http://localhost:9000"
        val exchangedToken = "123abc"
        val targetApp = "meroppfolging-backend-test"
        val userToken = "token123"
        val veilarbregistreringClient =
            VeilarbregistreringClient(tokendingsClient, baseUrl, targetApp, true, mockk(relaxed = true))

        val senOppfolgingRegistrering =
            SenOppfolgingRegistrering(
                besvarelse = Besvarelse(
                    utdanning = UtdanningSvar.INGEN_UTDANNING,
                    utdanningBestatt = UtdanningBestattSvar.JA,
                    utdanningGodkjent = UtdanningGodkjentSvar.NEI,
                    andreForhold = AndreForholdSvar.NEI,
                    sisteStilling = SisteStillingSvar.INGEN_SVAR,
                    fremtidigSituasjon = FremtidigSituasjonSvar.USIKKER,
                    tilbakeIArbeid = TilbakeIArbeidSvar.JA_FULL_STILLING,
                ),
                teksterForBesvarelse = listOf(
                    TekstForSporsmal(
                        "utdanning",
                        "Hva er din høyeste fullførte utdanning?",
                        "Høyere utdanning (1 til 4 år)",
                    ),
                    TekstForSporsmal(
                        "utdanningBestatt",
                        "Er utdanningen din bestått?",
                        "Ja",
                    ),
                    TekstForSporsmal(
                        "utdanningGodkjent",
                        "Er utdanningen din godkjent i Norge?",
                        "Ja",
                    ),
                    TekstForSporsmal(
                        "andreForhold",
                        "Er det noe annet enn helsen din som NAV bør ta hensyn til?",
                        "Nei",
                    ),
                    TekstForSporsmal(
                        "sisteStilling",
                        "-",
                        "-",
                    ),
                    TekstForSporsmal(
                        "fremtidigSituasjon",
                        "Hva tenker du om din fremtidige situasjon?",
                        "Jeg trenger ny jobb",
                    ),
                    TekstForSporsmal(
                        "tilbakeIArbeid",
                        "Tror du at du kommer tilbake i jobb før du har vært sykmeldt i 52 uker?",
                        "Ingen svar",
                    ),
                ),
            )
        val startRegistrationDTO =
            StartRegistrationDTO(
                StartRegistrationType.SYKMELDT_REGISTRERING,
                "TEST",
                "TEST2",
                "TEST3",
            )

        val veilarbregistreringServer = WireMockServer(9000)
        listener(WireMockListener(veilarbregistreringServer, ListenerMode.PER_TEST))

        beforeTest {
            every { tokendingsClient.exchangeToken(userToken, targetApp) } returns exchangedToken
            veilarbregistreringServer.resetAll()
        }

        test("Happy path complete registration") {
            veilarbregistreringServer.stubCompleteRegistration(
                exchangedToken,
            )
            veilarbregistreringClient.completeRegistration(userToken, senOppfolgingRegistrering)
            veilarbregistreringServer.verifyCompleteRegistration()
        }

        test("Happy path start registration") {
            veilarbregistreringServer.stubStartRegistration(
                exchangedToken,
                startRegistrationDTO,
            )
            val result = veilarbregistreringClient.startRegistration(userToken)
            result shouldBe startRegistrationDTO
        }

        test("StartRegistration with only mandatory fields") {
            val startRegistrationDTOWithNulls =
                StartRegistrationDTO(
                    StartRegistrationType.SYKMELDT_REGISTRERING,
                    null,
                    null,
                    null,
                )
            veilarbregistreringServer.stubStartRegistration(
                exchangedToken,
                startRegistrationDTOWithNulls,
            )
            val result = veilarbregistreringClient.startRegistration(userToken)
            result shouldBe startRegistrationDTOWithNulls
        }

        test("Should not complete registration when toggle is false") {

            val veilarbregistreringClientWithToggleOff =
                VeilarbregistreringClient(tokendingsClient, baseUrl, targetApp, false, mockk(relaxed = true))

            veilarbregistreringServer.stubCompleteRegistration(
                exchangedToken,
            )
            veilarbregistreringClientWithToggleOff.completeRegistration(userToken, senOppfolgingRegistrering)
            veilarbregistreringServer.verifyCompleteRegistrationNotCalled()
        }
    },
)

fun WireMockServer.stubCompleteRegistration(token: String) {
    this.stubFor(
        post(urlPathEqualTo(VEILARBREGISTRERING_COMPLETE_PATH))
            .withHeader(HttpHeaders.AUTHORIZATION, containing(token))
            .willReturn(
                aResponse()
                    .withStatus(200),
            ),
    )
}

fun WireMockServer.verifyCompleteRegistration() {
    this.verify(postRequestedFor(urlPathEqualTo(VEILARBREGISTRERING_COMPLETE_PATH)))
}

fun WireMockServer.verifyCompleteRegistrationNotCalled() {
    this.verify(0, postRequestedFor(urlPathEqualTo(VEILARBREGISTRERING_COMPLETE_PATH)))
}

fun WireMockServer.stubStartRegistration(
    token: String,
    startRegistrationDTO: StartRegistrationDTO,
) {
    this.stubFor(
        get(urlPathEqualTo(VEILARBREGISTRERING_START_PATH))
            .withHeader(HttpHeaders.AUTHORIZATION, containing(token))
            .willReturn(
                aResponse()
                    .withBody(
                        jacksonMapper().writeValueAsString(
                            startRegistrationDTO,
                        ),
                    )
                    .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .withStatus(200),
            ),
    )
}
