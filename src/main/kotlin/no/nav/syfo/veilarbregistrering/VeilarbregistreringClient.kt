package no.nav.syfo.veilarbregistrering

import no.nav.syfo.NAV_CALL_ID_HEADER
import no.nav.syfo.auth.bearerHeader
import no.nav.syfo.auth.tokendings.TokendingsClient
import no.nav.syfo.createCallId
import no.nav.syfo.logger
import no.nav.syfo.oppfolgingstilfelle.RequestUnauthorizedException
import no.nav.syfo.senoppfolging.SykmeldtRegistrering
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

const val VEILARBREGISTRERING_COMPLETE_PATH = "/veilarbregistrering/api/fullfoersykmeldtregistrering"
const val VEILARBREGISTRERING_START_PATH = "/veilarbregistrering/api/startregistrering"

@Service
class VeilarbregistreringClient(
    private val tokenDingsClient: TokendingsClient,
    @Value("\${veilarbregistrering.url}") private val baseUrl: String,
    @Value("\${veilarbregistrering.id}") private var targetApp: String,
) {
    private val log = logger()

    fun startRegistration(token: String): StartRegistrationDTO {
        val exchangedToken =
            tokenDingsClient.exchangeToken(
                token,
                targetApp,
            )
        val httpEntity = createHttpEntity(exchangedToken)

        try {
            return RestTemplate().exchange(
                "$baseUrl$VEILARBREGISTRERING_START_PATH",
                HttpMethod.GET,
                httpEntity,
                StartRegistrationDTO::class.java,
            ).body ?: throw IllegalStateException("Can't do this")
        } catch (e: RestClientResponseException) {
            handleException(e, httpEntity)
        }
    }

    fun completeRegistration(
        token: String,
        sykmeldtRegistrering: SykmeldtRegistrering,
    ) {
        val exchangedToken =
            tokenDingsClient.exchangeToken(
                token,
                targetApp,
            )
        val httpEntity = createHttpEntity(exchangedToken, sykmeldtRegistrering)

        try {
            RestTemplate().postForEntity("$baseUrl${VEILARBREGISTRERING_COMPLETE_PATH}", httpEntity, String::class.java)
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

    private fun createHttpEntity(
        exchangedToken: String,
        sykmeldtRegistrering: SykmeldtRegistrering,
    ): HttpEntity<*> {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, bearerHeader(exchangedToken))
        headers.add(NAV_CALL_ID_HEADER, createCallId())
        return HttpEntity<Any>(sykmeldtRegistrering, headers)
    }

    private fun handleException(
        e: RestClientResponseException,
        httpEntity: HttpEntity<*>,
    ): Nothing {
        if (e.statusCode.isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
            throw RequestUnauthorizedException(
                "Unauthorized request when calling veilarbregistrering",
            )
        } else {
            log.error(
                "Error requesting veilarbregistrering with callId {}: ",
                httpEntity.headers[NAV_CALL_ID_HEADER],
                e,
            )
            throw e
        }
    }
}
