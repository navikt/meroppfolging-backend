package no.nav.syfo.behandlendeenhet

import no.nav.syfo.MEROPPFOLGING_BACKEND_CONSUMER_ID
import no.nav.syfo.NAV_CALL_ID_HEADER
import no.nav.syfo.NAV_CONSUMER_ID_HEADER
import no.nav.syfo.NAV_PERSONIDENT_HEADER
import no.nav.syfo.auth.azuread.AzureAdClient
import no.nav.syfo.auth.bearerHeader
import no.nav.syfo.behandlendeenhet.domain.BehandlendeEnhet
import no.nav.syfo.createCallId
import no.nav.syfo.logger
import no.nav.syfo.oppfolgingstilfelle.RequestUnauthorizedException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

const val BEHANDLENDEENHET_PATH = "/api/system/v2/personident"

@Service
class BehandlendeEnhetClient(
    private val azureAdClient: AzureAdClient,
    @Value("\${syfobehandlendeenhet.url}") private val baseUrl: String,
    @Value("\${syfobehandlendeenhet.id}") private var targetApp: String,
) {
    private val log = logger()

    fun getBehandlendeEnhet(personIdent: String): BehandlendeEnhet {
        val exchangedToken = azureAdClient.getSystemToken(targetApp)
        val httpEntity = createHttpEntity(exchangedToken, personIdent)

        return try {
            val response = getResponse(httpEntity)
            response.body!!
        } catch (e: RestClientResponseException) {
            handleException(e, httpEntity)
        }
    }

    private fun createHttpEntity(exchangedToken: String, personIdent: String): HttpEntity<*> {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, bearerHeader(exchangedToken))
        headers.add(NAV_CALL_ID_HEADER, createCallId())
        headers[NAV_CONSUMER_ID_HEADER] = MEROPPFOLGING_BACKEND_CONSUMER_ID
        headers.add(NAV_PERSONIDENT_HEADER, personIdent)
        return HttpEntity<Any>(headers)
    }

    private fun getResponse(httpEntity: HttpEntity<*>): ResponseEntity<BehandlendeEnhet> {
        return RestTemplate().exchange(
            "$baseUrl$BEHANDLENDEENHET_PATH",
            HttpMethod.GET,
            httpEntity,
            BehandlendeEnhet::class.java,
        )
    }

    private fun handleException(
        e: RestClientResponseException,
        httpEntity: HttpEntity<*>,
    ): Nothing {
        if (e.statusCode.isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
            throw RequestUnauthorizedException(
                "Unauthorized request to get behandlendeenhet from syfobehandlendeenhet",
            )
        } else {
            log.error(
                "Error requesting behandlendeenhet from syfobehandlendeenhet with callId {}: ",
                httpEntity.headers[NAV_CALL_ID_HEADER],
                e,
            )
            throw e
        }
    }
}
