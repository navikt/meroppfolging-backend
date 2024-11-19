package no.nav.syfo.dokarkiv

import no.nav.syfo.MEROPPFOLGING_BACKEND_CONSUMER_ID
import no.nav.syfo.NAV_CALL_ID_HEADER
import no.nav.syfo.NAV_CONSUMER_ID_HEADER
import no.nav.syfo.auth.azuread.AzureAdClient
import no.nav.syfo.auth.bearerHeader
import no.nav.syfo.createCallId
import no.nav.syfo.dokarkiv.domain.AvsenderMottaker
import no.nav.syfo.dokarkiv.domain.DokarkivRequest
import no.nav.syfo.dokarkiv.domain.DokarkivResponse
import no.nav.syfo.dokarkiv.domain.Dokument
import no.nav.syfo.dokarkiv.domain.Dokumentvariant
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Service
class DokarkivClient(
    private val azureAdClient: AzureAdClient,
    @Value("\${dokarkiv.url}") private val dokarkivUrl: String,
    @Value("\${dokarkiv.scope}") private val dokarkivScope: String,
) {
    private val journalpostPath = "/rest/journalpostapi/v1/journalpost"
    private val journalpostParamString = "forsoekFerdigstill"
    private val journalpostParamValue = true

    val url = UriComponentsBuilder.fromHttpUrl(dokarkivUrl)
        .path(journalpostPath)
        .queryParam(journalpostParamString, journalpostParamValue)
        .toUriString()

    private val log = LoggerFactory.getLogger(DokarkivClient::class.qualifiedName)

    fun postDocumentToDokarkiv(fnr: String, pdf: ByteArray, uuid: String, title: String, filnavnBeforeUUID: String):
        DokarkivResponse? {
        return try {
            val token = azureAdClient.getSystemToken(dokarkivScope)

            val dokarkivRequest = createDokarkivRequest(pdf, uuid, filnavnBeforeUUID, title, fnr)

            val response = RestTemplate().postForEntity(
                url,
                createHttpEntity(
                    token,
                    dokarkivRequest,
                ),
                DokarkivResponse::class.java,
            )

            when (response.statusCode) {
                HttpStatus.CREATED -> {
                    log.info("Sending to dokarkiv successful, journalpost created")
                    response.body!!
                }

                HttpStatus.CONFLICT -> {
                    log.info("Sending to dokarkiv successful, journalpost was created before")
                    response.body!!
                }

                HttpStatus.UNAUTHORIZED -> {
                    log.error("Failed to post document to Dokarkiv: Unable to authorize")
                    null
                }

                else -> {
                    log.error("Failed to post document to Dokarkiv: $response")
                    null
                }
            }
        } catch (e: HttpClientErrorException) {
            log.error("Client error while posting document to Dokarkiv, message: ${e.message}, cause: ${e.cause}")
            null
        } catch (e: HttpServerErrorException) {
            log.error("Server error while posting document to Dokarkiv, message: ${e.message}, cause: ${e.cause}")

            null
        } catch (e: ResourceAccessException) {
            log.error(
                "Resource access error while posting document to Dokarkiv, " +
                    "message: ${e.message}, cause: ${e.cause}"
            )
            null
        } catch (e: RestClientException) {
            log.error(
                "Unexpected error while posting document to Dokarkiv, message: ${e.message}, cause: ${e.cause}"
            )
            null
        }
    }

    private fun createDokarkivRequest(
        pdf: ByteArray,
        uuid: String,
        filnavnBeforeUUID: String,
        title: String,
        fnr: String,
    ): DokarkivRequest {
        val dokumentvarianter = listOf(Dokumentvariant.create(pdf, uuid, filnavnBeforeUUID))
        val dokumenter = listOf(
            Dokument.create(
                dokumentvarianter = dokumentvarianter,
                tittel = title,
            ),
        )
        val dokarkivRequest = DokarkivRequest.create(
            avsenderMottaker = AvsenderMottaker.create(fnr),
            dokumenter = dokumenter,
            uuid = uuid,
            tittel = title,
        )
        return dokarkivRequest
    }

    private fun createHttpEntity(
        exchangedToken: String,
        request: DokarkivRequest,
    ): HttpEntity<*> {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, bearerHeader(exchangedToken))
        headers.add(NAV_CALL_ID_HEADER, createCallId())
        headers.add(NAV_CONSUMER_ID_HEADER, MEROPPFOLGING_BACKEND_CONSUMER_ID)
        return HttpEntity<Any>(request, headers)
    }
}
