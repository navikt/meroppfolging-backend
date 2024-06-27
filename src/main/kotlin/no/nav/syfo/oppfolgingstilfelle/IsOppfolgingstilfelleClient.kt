package no.nav.syfo.oppfolgingstilfelle

import no.nav.syfo.NAV_CALL_ID_HEADER
import no.nav.syfo.auth.bearerHeader
import no.nav.syfo.auth.tokendings.TokendingsClient
import no.nav.syfo.createCallId
import no.nav.syfo.exception.RequestUnauthorizedException
import no.nav.syfo.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import java.time.LocalDate

const val ISOPPFOLGINGSTILFELLE_PATH = "/api/v1/arbeidstaker/oppfolgingstilfelle"

@Service
class IsOppfolgingstilfelleClient(
    private val tokenDingsClient: TokendingsClient,
    @Value("\${isoppfolgingstilfelle.url}") private val baseUrl: String,
    @Value("\${isoppfolgingstilfelle.id}") private var targetApp: String,
) {
    private val log = logger()

    fun isSykmeldt(token: String): Boolean {
        val newestOppfolgingstilfelle = getOppfolgingstilfeller(token).firstOrNull()
        return newestOppfolgingstilfelle?.end?.isAfter(LocalDate.now().minusDays(1)) ?: false
    }

    fun getOppfolgingstilfeller(token: String): List<Oppfolgingstilfelle> {
        val exchangedToken =
            tokenDingsClient.exchangeToken(
                token,
                targetApp,
            )
        val httpEntity = createHttpEntity(exchangedToken)

        return try {
            val response = getResponse(httpEntity)
            response.body!!
        } catch (e: RestClientResponseException) {
            handleException(e, httpEntity)
        }
    }

    private fun createHttpEntity(exchangedToken: String): HttpEntity<*> {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, bearerHeader(exchangedToken))
        headers.add(NAV_CALL_ID_HEADER, createCallId())
        return HttpEntity<Any>(headers)
    }

    private fun getResponse(httpEntity: HttpEntity<*>): ResponseEntity<List<Oppfolgingstilfelle>> {
        return RestTemplate().exchange(
            "$baseUrl$ISOPPFOLGINGSTILFELLE_PATH",
            HttpMethod.GET,
            httpEntity,
            object : ParameterizedTypeReference<List<Oppfolgingstilfelle>>() {},
        )
    }

    private fun handleException(
        e: RestClientResponseException,
        httpEntity: HttpEntity<*>,
    ): Nothing {
        if (e.statusCode.isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
            throw RequestUnauthorizedException(
                "Unauthorized request to get oppfolgingstilfeller from isoppfolgingstilfelle",
            )
        } else {
            log.error(
                "Error requesting oppfolgingstilfelle from isoppfolgingstilfelle with callId {}: ",
                httpEntity.headers[NAV_CALL_ID_HEADER],
                e,
            )
            throw e
        }
    }
}
