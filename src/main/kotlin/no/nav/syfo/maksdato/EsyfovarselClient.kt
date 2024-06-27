package no.nav.syfo.maksdato

import no.nav.syfo.NAV_CALL_ID_HEADER
import no.nav.syfo.auth.bearerHeader
import no.nav.syfo.auth.tokendings.TokendingsClient
import no.nav.syfo.createCallId
import no.nav.syfo.exception.RequestUnauthorizedException
import no.nav.syfo.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

const val MAXDATE_PATH = "/api/v1/sykepenger/maxdate"

@Service
class EsyfovarselClient(
    private val tokenDingsClient: TokendingsClient,
    @Value("\${esyfovarsel.url}") private val baseUrl: String,
    @Value("\${esyfovarsel.id}") private var targetApp: String,
) {
    private val log = logger()

    fun getMaxDate(token: String): String? {
        val exchangedToken =
            tokenDingsClient.exchangeToken(
                token,
                targetApp,
            )
        val httpEntity = createHttpEntity(exchangedToken)

        return try {
            val response = getResponse(httpEntity)
            response?.maxDate
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

    private fun getResponse(httpEntity: HttpEntity<*>): SykepengerMaxDateResponse? {
        return RestTemplate().exchange(
            "$baseUrl$MAXDATE_PATH",
            HttpMethod.GET,
            httpEntity,
            SykepengerMaxDateResponse::class.java,
        ).body
    }

    private fun handleException(
        e: RestClientResponseException,
        httpEntity: HttpEntity<*>,
    ): Nothing {
        if (e.statusCode.isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
            throw RequestUnauthorizedException(
                "Unauthorized request to get maxdate from esyfovarsel",
            )
        } else {
            log.error(
                "Error requesting maxdate from esyfovarsel with callId {}: ",
                httpEntity.headers[NAV_CALL_ID_HEADER],
                e,
            )
            throw e
        }
    }
}
