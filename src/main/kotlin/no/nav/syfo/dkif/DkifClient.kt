package no.nav.syfo.dkif

import no.nav.syfo.NAV_CALL_ID_HEADER
import no.nav.syfo.NAV_PERSONIDENT_HEADER
import no.nav.syfo.auth.azuread.AzureAdClient
import no.nav.syfo.auth.bearerHeader
import no.nav.syfo.createCallId
import no.nav.syfo.dkif.domain.Kontaktinfo
import no.nav.syfo.exception.RequestUnauthorizedException
import no.nav.syfo.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

@Service
class DkifClient(
    private val azureAdClient: AzureAdClient,
    @Value("\${dkif.url}") private val dkifUrl: String,
    @Value("\${dkif.scope}") private val dkifScope: String,
) {

    private val logger = logger()

    fun person(fnr: String): Kontaktinfo {
        val token = azureAdClient.getSystemToken(dkifScope)
        val httpEntity = createHttpEntity(token, fnr)

        try {
            val responseBody = RestTemplate().exchange(
                dkifUrl,
                HttpMethod.GET,
                httpEntity,
                Kontaktinfo::class.java,
            ).body

            checkNotNull(responseBody) { "Response body is null" }

            return responseBody
        } catch (e: RestClientResponseException) {
            logger.error("Error while calling DKIF: ${e.message}", e)
            handleException(e, httpEntity)
        }
    }

    private fun createHttpEntity(
        token: String,
        fnr: String,
    ): HttpEntity<*> {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, bearerHeader(token))
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
        headers.add(NAV_CALL_ID_HEADER, createCallId())
        headers.add(NAV_PERSONIDENT_HEADER, fnr)
        return HttpEntity<Any>(headers)
    }

    private fun handleException(
        e: RestClientResponseException,
        httpEntity: HttpEntity<*>,
    ): Nothing {
        if (e.statusCode.isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
            throw RequestUnauthorizedException(
                "Could not get kontaktinfo from DKIF: Unable to authorize",
            )
        } else {
            logger.error(
                "Error requesting kontaktinfo from DKIF: callId {}: ",
                httpEntity.headers[NAV_CALL_ID_HEADER],
                e,
            )
            throw e
        }
    }
}
