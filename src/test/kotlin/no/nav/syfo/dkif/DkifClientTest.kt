package no.nav.syfo.dkif

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.wiremock.ListenerMode
import io.kotest.extensions.wiremock.WireMockListener
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.auth.azuread.AzureAdClient
import no.nav.syfo.dkif.domain.Kontaktinfo
import no.nav.syfo.dkif.domain.PostPersonerResponse
import no.nav.syfo.exception.DkifRequestFailedException
import org.springframework.http.HttpStatus
import java.util.UUID

const val BASE_URL = "http://localhost:9000"
const val REST_PATH = "/rest/v1/personer"

class DkifClientTest : FunSpec(
    {
        val azureAdTokenConsumer = mockk<AzureAdClient>()
        val dkifScope = "some-scope"
        val dkifUrl = "$BASE_URL$REST_PATH"

        val validFnr = "12345678910"
        val unknownFnr = "01987654321"
        val dkifClient = DkifClient(azureAdClient = azureAdTokenConsumer, dkifScope = dkifScope, dkifUrl = dkifUrl)
        val krrServer = WireMockServer(9000)
        listener(WireMockListener(krrServer, ListenerMode.PER_TEST))
        beforeTest {
            every { azureAdTokenConsumer.getSystemToken(dkifScope) } returns UUID.randomUUID().toString()
        }

        test("Throws error on request when fnr is not included in response") {
            krrServer.stubPersonerResponse(
                PostPersonerResponse(
                    personer = emptyMap(),
                    feil = mapOf(unknownFnr to "Not Found"),
                ),
            )
            val exception = shouldThrow<DkifRequestFailedException> {
                dkifClient.person(unknownFnr)
            }
            exception.message shouldContain "Response did not contain person"
        }

        test("Throws error with did not contain person when response contains json with with invalid contract") {
            krrServer.stubPersonerWithCustomResponse(
                response = mapOf("what" to "ever"),
                HttpStatus.OK,
            )
            val exception = shouldThrow<DkifRequestFailedException> {
                dkifClient.person(validFnr)
            }
            exception.message shouldContain "Response did not contain person"
        }

        test("Throws error with message for unexpected status on response") {
            krrServer.stubPersonerWithCustomResponse(
                response = mapOf("what" to "ever"),
                HttpStatus.ACCEPTED,
            )
            val exception = shouldThrow<DkifRequestFailedException> {
                dkifClient.person(validFnr)
            }
            exception.message shouldContain "Received response with status code: ${HttpStatus.ACCEPTED}"
        }

        test("Returns KontaktInfo on successful request") {
            val digitalKontaktInfo = Kontaktinfo(
                reservert = false,
                kanVarsles = true,
            )
            krrServer.stubPersonerResponse(
                PostPersonerResponse(
                    personer = mapOf(validFnr to digitalKontaktInfo),
                    feil = emptyMap(),
                ),
            )
            val response = dkifClient.person(validFnr)
            response.reservert shouldBe digitalKontaktInfo.reservert
            response.kanVarsles shouldBe digitalKontaktInfo.kanVarsles
        }
    },
)

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

fun WireMockServer.stubPersonerResponse(response: PostPersonerResponse) {
    this.stubFor(
        WireMock.post(WireMock.urlPathEqualTo(REST_PATH)).willReturn(
            aResponse().withBody(objectMapper.writeValueAsString(response))
                .withHeader("Content-Type", "application/json").withStatus(200),
        ),
    )
}

fun WireMockServer.stubPersonerWithCustomResponse(response: Map<String, String>, statusCode: HttpStatus) {
    this.stubFor(
        WireMock.post(WireMock.urlPathEqualTo(REST_PATH)).willReturn(
            aResponse().withBody(objectMapper.writeValueAsString(response))
                .withHeader("Content-Type", "application/json").withStatus(statusCode.value()),
        ),
    )
}
