package no.nav.syfo.dokarkiv

import no.nav.syfo.MEROPPFOLGING_BACKEND_CONSUMER_ID
import no.nav.syfo.NAV_CALL_ID_HEADER
import no.nav.syfo.NAV_CONSUMER_ID_HEADER
import no.nav.syfo.auth.azuread.AzureAdClient
import no.nav.syfo.auth.bearerHeader
import no.nav.syfo.createCallId
import no.nav.syfo.dokarkiv.database.JournalforFailedDAO
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
    private val journalforFailedDAO: JournalforFailedDAO,
) {
    private val journalpostPath = "/rest/journalpostapi/v1/journalpost"
    private val journalpostParamString = "forsoekFerdigstill"
    private val journalpostParamValue = true

    val url = UriComponentsBuilder.fromHttpUrl(dokarkivUrl)
        .path(journalpostPath)
        .queryParam(journalpostParamString, journalpostParamValue)
        .toUriString()

    private val log = LoggerFactory.getLogger(DokarkivClient::class.qualifiedName)

    fun postDocumentToDokarkiv(fnr: String, pdf: ByteArray, uuid: String,): DokarkivResponse? {
        return try {
            val token = azureAdClient.getSystemToken(dokarkivScope)

            val response = RestTemplate().postForEntity(
                url,
                createHttpEntity(
                    token,
                    DokarkivRequest.create(
                        avsenderMottaker = AvsenderMottaker.create(fnr),
                        dokumenter = listOf(Dokument.create(listOf(Dokumentvariant.create(pdf, uuid)))),
                        uuid = uuid,
                    ),
                ),
                DokarkivResponse::class.java,
            )

            when (response.statusCode) {
                HttpStatus.CREATED -> {
                    log.info("Sending to dokarkiv successful, journalpost created")
                    response.body
                }

                HttpStatus.CONFLICT -> {
                    log.info("Sending to dokarkiv successful, journalpost was created before")
                    response.body
                }

                HttpStatus.UNAUTHORIZED -> {
                    log.error("Failed to post document to Dokarkiv: Unable to authorize")
                    journalforFailedDAO.persistJournalforFailed(fnr, pdf, uuid, HttpStatus.UNAUTHORIZED.name)
                    null
                }

                else -> {
                    log.error("Failed to post document to Dokarkiv: $response")
                    journalforFailedDAO.persistJournalforFailed(fnr, pdf, uuid, response.body.toString())
                    null
                }
            }
        } catch (e: HttpClientErrorException) {
            val message = "Client error while posting document to Dokarkiv, message: ${e.message}, cause: ${e.cause}"
            log.error(message)
            journalforFailedDAO.persistJournalforFailed(fnr, pdf, uuid, message)
            null
        } catch (e: HttpServerErrorException) {
            val message = "Server error while posting document to Dokarkiv, message: ${e.message}, cause: ${e.cause}"
            log.error(message)
            journalforFailedDAO.persistJournalforFailed(fnr, pdf, uuid, message)

            null
        } catch (e: ResourceAccessException) {
            val message = "Resource access error while posting document to Dokarkiv, " +
                "message: ${e.message}, cause: ${e.cause}"
            log.error(message)
            journalforFailedDAO.persistJournalforFailed(fnr, pdf, uuid, message)
            null
        } catch (e: RestClientException) {
            val message =
                "Unexpected error while posting document to Dokarkiv, message: ${e.message}, cause: ${e.cause}"
            log.error(message)
            journalforFailedDAO.persistJournalforFailed(fnr, pdf, uuid, message)
            null
        }
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
