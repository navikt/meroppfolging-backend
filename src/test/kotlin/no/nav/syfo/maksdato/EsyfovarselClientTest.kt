package no.nav.syfo.maksdato

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.wiremock.ListenerMode
import io.kotest.extensions.wiremock.WireMockListener
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.auth.tokendings.TokendingsClient
import org.springframework.http.HttpHeaders

class EsyfovarselClientTest : FunSpec(
    {
        val tokendingsClient: TokendingsClient = mockk<TokendingsClient>()
        val baseUrl = "http://localhost:9000"
        val exchangedToken = "123abc"
        val targetApp = "meroppfolging-backend-test"
        val userToken = "token123"
        val sykepengedagerInformasjonClient = SykepengedagerInformasjonClient(tokendingsClient, baseUrl, targetApp)

        val esyfovarselServer = WireMockServer(9000)
        listener(WireMockListener(esyfovarselServer, ListenerMode.PER_TEST))

        beforeTest {
            every { tokendingsClient.exchangeToken(userToken, targetApp) } returns exchangedToken
        }

        test("Get maksdato") {
            esyfovarselServer.stubMaxDate(
                exchangedToken,
                SykepengerMaxDateResponse("3.juni 2023", "2023-12-31", "61"),
            )
            val result = sykepengedagerInformasjonClient.getSykepengerMaxDateResponse(userToken)
            result?.maxDate shouldBe "3.juni 2023"
        }

        test("Get maksdato null") {
            esyfovarselServer.stubMaxDate(
                exchangedToken,
                SykepengerMaxDateResponse(null, "2023-12-31", "44"),
            )
            val result = sykepengedagerInformasjonClient.getSykepengerMaxDateResponse(userToken)
            result?.maxDate shouldBe null
        }

        test("Get maksdato and utbetaltTom null") {
            esyfovarselServer.stubMaxDate(
                exchangedToken,
                SykepengerMaxDateResponse(null, null, null),
            )
            val result = sykepengedagerInformasjonClient.getSykepengerMaxDateResponse(userToken)
            result?.maxDate shouldBe null
        }

        test("Get maksdato when utbetaltTom is null") {
            esyfovarselServer.stubMaxDate(
                exchangedToken,
                SykepengerMaxDateResponse("3.juni 2023", null, "55"),
            )
            val result = sykepengedagerInformasjonClient.getSykepengerMaxDateResponse(userToken)
            result?.maxDate shouldBe "3.juni 2023"
        }
    },
)

fun WireMockServer.stubMaxDate(
    token: String,
    maxDateResponses: SykepengerMaxDateResponse,
) {
    this.stubFor(
        get(urlPathEqualTo(MAXDATE_PATH))
            .withHeader(HttpHeaders.AUTHORIZATION, containing(token))
            .willReturn(
                aResponse()
                    .withBody(objectMapper.writeValueAsString(maxDateResponses))
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200),
            ),
    )
}

val objectMapper: ObjectMapper =
    ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
