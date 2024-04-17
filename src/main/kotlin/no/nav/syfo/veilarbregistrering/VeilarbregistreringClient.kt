package no.nav.syfo.veilarbregistrering

import no.nav.syfo.MEROPPFOLGING_BACKEND_CONSUMER_ID
import no.nav.syfo.NAV_CALL_ID_HEADER
import no.nav.syfo.NAV_CONSUMER_ID
import no.nav.syfo.auth.bearerHeader
import no.nav.syfo.auth.tokendings.TokendingsClient
import no.nav.syfo.createCallId
import no.nav.syfo.logger
import no.nav.syfo.metric.Metric
import no.nav.syfo.oppfolgingstilfelle.RequestUnauthorizedException
import no.nav.syfo.senoppfolging.domain.SenOppfolgingRegistrering
import no.nav.syfo.veilarbregistrering.domain.StartRegistrationDTO
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
    private val metric: Metric,
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
            val responseBody = RestTemplate().exchange(
                "$baseUrl$VEILARBREGISTRERING_START_PATH",
                HttpMethod.GET,
                httpEntity,
                StartRegistrationDTO::class.java,
            ).body

            checkNotNull(responseBody) { "Response body is null" }

            return responseBody
        } catch (e: RestClientResponseException) {
            handleException(e, httpEntity)
        }
    }

    fun completeRegistration(
        token: String,
        senOppfolgingRegistrering: SenOppfolgingRegistrering,
    ) {
        val exchangedToken =
            tokenDingsClient.exchangeToken(
                token,
                targetApp,
            )
        val httpEntity = createHttpEntity(exchangedToken, senOppfolgingRegistrering)

        try {
            RestTemplate().postForEntity("$baseUrl${VEILARBREGISTRERING_COMPLETE_PATH}", httpEntity, String::class.java)
            metric.countCallVeilarbregistreringComplete()
        } catch (e: RestClientResponseException) {
            handleException(e, httpEntity)
        }
    }

    private fun createHttpEntity(
        exchangedToken: String,
        senOppfolgingRegistrering: SenOppfolgingRegistrering? = null,
    ): HttpEntity<*> {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, bearerHeader(exchangedToken))
        headers.add(NAV_CALL_ID_HEADER, createCallId())
        headers.add(NAV_CONSUMER_ID, MEROPPFOLGING_BACKEND_CONSUMER_ID)
        return HttpEntity<Any>(senOppfolgingRegistrering, headers)
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
