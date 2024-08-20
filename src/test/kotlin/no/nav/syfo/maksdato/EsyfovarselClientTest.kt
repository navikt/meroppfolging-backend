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
        val esyfovarselClient = EsyfovarselClient(tokendingsClient, baseUrl, targetApp)

        val esyfovarselServer = WireMockServer(9000)
        listener(WireMockListener(esyfovarselServer, ListenerMode.PER_TEST))

        beforeTest {
            every { tokendingsClient.exchangeToken(userToken, targetApp) } returns exchangedToken
        }

        test("Get maksdato") {
            val response = SykepengerMaxDateResponse("3.juni 2023", "2023-12-31", "71")
            esyfovarselServer.stubMaxDate(
                exchangedToken,
                response,
            )
            val result = esyfovarselClient.getSykepengerMaxDateResponse(userToken)
            result shouldBe response
        }

        test("Get maksdato null") {
            val response = SykepengerMaxDateResponse(null, "2023-12-31", "52")
            esyfovarselServer.stubMaxDate(
                exchangedToken,
                response,
            )
            val result = esyfovarselClient.getSykepengerMaxDateResponse(userToken)
            result?.maxDate shouldBe null
        }

        test("Get maksdato and utbetaltTom null") {
            val response = SykepengerMaxDateResponse(null, null, "55")
            esyfovarselServer.stubMaxDate(
                exchangedToken,
                response,
            )
            val result = esyfovarselClient.getSykepengerMaxDateResponse(userToken)
            result?.maxDate shouldBe null
        }

        test("Get maksdato when utbetaltTom is null") {
            val response = SykepengerMaxDateResponse("3.juni 2023", null, "35")
            esyfovarselServer.stubMaxDate(
                exchangedToken,
                response,
            )
            val result = esyfovarselClient.getSykepengerMaxDateResponse(userToken)
            result shouldBe response
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
