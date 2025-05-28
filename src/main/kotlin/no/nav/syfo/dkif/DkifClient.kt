package no.nav.syfo.dkif

import no.nav.syfo.NAV_CALL_ID_HEADER
import no.nav.syfo.auth.azuread.AzureAdClient
import no.nav.syfo.auth.bearerHeader
import no.nav.syfo.createCallId
import no.nav.syfo.dkif.domain.Kontaktinfo
import no.nav.syfo.dkif.domain.PostPersonerRequest
import no.nav.syfo.dkif.domain.PostPersonerResponse
import no.nav.syfo.exception.DkifRequestFailedException
import no.nav.syfo.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
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

        val response = RestTemplate().exchange(
            dkifUrl,
            HttpMethod.POST,
            httpEntity,
            PostPersonerResponse::class.java,
        )
        if (response.statusCode != HttpStatus.OK) {
            logAndThrowError("Received response with status code: ${response.statusCode}")
        }

        checkNotNull(response.body) { "Response body is null" }
        val kontaktinfo = response.body?.let {
            it.personer.getOrDefault(fnr, null)
                ?: logAndThrowError("Response did not contain person")
        } ?: logAndThrowError("ResponseBody is null")
        return kontaktinfo
    }

    private fun createHttpEntity(
        token: String,
        fnr: String,
    ): HttpEntity<PostPersonerRequest> {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, bearerHeader(token))
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
        headers.add(NAV_CALL_ID_HEADER, createCallId())
        return HttpEntity(PostPersonerRequest(setOf(fnr)), headers)
    }

    private fun logAndThrowError(message: String): Nothing {
        logger.error(message)
        throw DkifRequestFailedException(message)
    }
}
