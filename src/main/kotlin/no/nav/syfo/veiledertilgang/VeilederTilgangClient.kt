package no.nav.syfo.veiledertilgang

import no.nav.syfo.MEROPPFOLGING_BACKEND_CONSUMER_ID
import no.nav.syfo.NAV_CALL_ID_HEADER
import no.nav.syfo.NAV_CONSUMER_ID_HEADER
import no.nav.syfo.NAV_PERSONIDENT_HEADER
import no.nav.syfo.auth.azuread.AzureAdClient
import no.nav.syfo.auth.bearerHeader
import no.nav.syfo.createCallId
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.exception.RequestUnauthorizedException
import no.nav.syfo.logger
import no.nav.syfo.veiledertilgang.domain.Tilgang
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

const val TILGANGSKONTROLL_PERSON_PATH = "/api/tilgang/navident/person"

@Service
class VeilederTilgangClient(
    private val azureAdClient: AzureAdClient,
    @Value("\${istilgangskontroll.url}") private val baseUrl: String,
    @Value("\${istilgangskontroll.id}") private var targetApp: String
) {

    private val log = logger()

    fun hasVeilederTilgangToPerson(token: String, personident: PersonIdentNumber): Boolean {
        val exchangedToken = azureAdClient.getOnBehalfOfToken(scopeClientId = targetApp, token = token)

        val httpEntity = createHttpEntity(
            exchangedToken = exchangedToken,
            personIdent = personident.value,
        )

        return try {
            val response = getResponse(httpEntity)
            response.body!!.erGodkjent
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

    private fun getResponse(httpEntity: HttpEntity<*>): ResponseEntity<Tilgang> = RestTemplate().exchange(
        "$baseUrl$TILGANGSKONTROLL_PERSON_PATH",
        HttpMethod.GET,
        httpEntity,
        Tilgang::class.java,
    )

    private fun handleException(
        e: RestClientResponseException,
        httpEntity: HttpEntity<*>,
    ): Nothing {
        if (e.statusCode.isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
            throw RequestUnauthorizedException(
                "Unauthorized request to istilgangskontroll",
            )
        } else {
            log.error(
                "Error requesting tilgang from istilgangskontroll with callId {}: ",
                httpEntity.headers[NAV_CALL_ID_HEADER],
                e,
            )
            throw e
        }
    }
}
