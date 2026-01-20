package no.nav.syfo.isoppfolgingstilfelle

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
import no.nav.syfo.oppfolgingstilfelle.ISOPPFOLGINGSTILFELLE_PATH
import no.nav.syfo.oppfolgingstilfelle.IsOppfolgingstilfelleClient
import no.nav.syfo.oppfolgingstilfelle.Oppfolgingstilfelle
import org.springframework.http.HttpHeaders
import java.time.LocalDate

class IsoppfolgingstilfelleClientTest :
    FunSpec(
        {
            val tokendingsClient: TokendingsClient = mockk<TokendingsClient>()
            val baseUrl = "http://localhost:9000"
            val exchangedToken = "123abc"
            val targetApp = "meroppfolging-backend-test"
            val userToken = "token123"
            val oppfolgingstilfelleSykmeldt =
                Oppfolgingstilfelle(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1))
            val oppfolgingstilfelleSykmeldtTidligere =
                Oppfolgingstilfelle(LocalDate.now().minusDays(10), LocalDate.now().minusDays(2))
            val isoppfolgingstilfelleClient = IsOppfolgingstilfelleClient(tokendingsClient, baseUrl, targetApp)

            val isoppfolgingstilfelleServer = WireMockServer(9000)
            listener(WireMockListener(isoppfolgingstilfelleServer, ListenerMode.PER_TEST))

            beforeTest {
                every { tokendingsClient.exchangeToken(userToken, targetApp) } returns exchangedToken
            }

            test("Aktivt oppfolgingstilfelle gir true") {
                isoppfolgingstilfelleServer.stubOppfolgingstilfelle(
                    exchangedToken,
                    listOf(oppfolgingstilfelleSykmeldt),
                )
                val result = isoppfolgingstilfelleClient.isSykmeldt(userToken)
                result shouldBe true
            }

            test("Flere oppfolgingstilfeller med aktivt oppfolgingstilfelle gir true") {
                isoppfolgingstilfelleServer.stubOppfolgingstilfelle(
                    exchangedToken,
                    listOf(oppfolgingstilfelleSykmeldt, oppfolgingstilfelleSykmeldtTidligere),
                )
                val result = isoppfolgingstilfelleClient.isSykmeldt(userToken)
                result shouldBe true
            }

            test("Tidligere oppfolgingstilfelle gir false") {
                isoppfolgingstilfelleServer.stubOppfolgingstilfelle(
                    exchangedToken,
                    listOf(oppfolgingstilfelleSykmeldtTidligere),
                )
                val result = isoppfolgingstilfelleClient.isSykmeldt(userToken)
                result shouldBe false
            }

            test("Ingen oppfolgingstilfelle gir false") {
                isoppfolgingstilfelleServer.stubOppfolgingstilfelle(
                    exchangedToken,
                    emptyList(),
                )
                val result = isoppfolgingstilfelleClient.isSykmeldt(userToken)
                result shouldBe false
            }
        },
    )

fun WireMockServer.stubOppfolgingstilfelle(token: String, oppfolgingstilfeller: List<Oppfolgingstilfelle>,) {
    this.stubFor(
        get(urlPathEqualTo(ISOPPFOLGINGSTILFELLE_PATH))
            .withHeader(HttpHeaders.AUTHORIZATION, containing(token))
            .willReturn(
                aResponse()
                    .withBody(objectMapper.writeValueAsString(oppfolgingstilfeller))
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
