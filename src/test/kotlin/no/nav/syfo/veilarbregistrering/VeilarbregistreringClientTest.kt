package no.nav.syfo.isoppfolgingstilfelle

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
import no.nav.syfo.senoppfolging.domain.Besvarelse
import no.nav.syfo.senoppfolging.domain.SenOppfolgingRegistrering
import no.nav.syfo.veilarbregistrering.VEILARBREGISTRERING_COMPLETE_PATH
import no.nav.syfo.veilarbregistrering.VEILARBREGISTRERING_START_PATH
import no.nav.syfo.veilarbregistrering.VeilarbregistreringClient
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
        val veilarbregistreringClient = VeilarbregistreringClient(tokendingsClient, baseUrl, targetApp)

        val senOppfolgingRegistrering = SenOppfolgingRegistrering(Besvarelse(), listOf())
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
                        objectMapper.writeValueAsString(
                            startRegistrationDTO,
                        ),
                    )
                    .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .withStatus(200),
            ),
    )
}
