package no.nav.syfo.dokarkiv

import SingleDocumentData
import net.logstash.logback.marker.Markers.appendEntries
import no.nav.syfo.MEROPPFOLGING_BACKEND_CONSUMER_ID
import no.nav.syfo.NAV_CALL_ID_HEADER
import no.nav.syfo.NAV_CONSUMER_ID_HEADER
import no.nav.syfo.auth.azuread.AzureAdClient
import no.nav.syfo.auth.bearerHeader
import no.nav.syfo.config.kafka.jacksonMapper
import no.nav.syfo.createCallId
import no.nav.syfo.documentFailureFields
import no.nav.syfo.dokarkiv.domain.AvsenderMottaker
import no.nav.syfo.dokarkiv.domain.Distribusjonskanal
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
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import tools.jackson.databind.ObjectMapper

@Service
class DokarkivClient(
    private val azureAdClient: AzureAdClient,
    @Value("\${dokarkiv.url}") private val dokarkivUrl: String,
    @Value("\${dokarkiv.scope}") private val dokarkivScope: String,
    private val objectMapper: ObjectMapper = jacksonMapper(),
) {
    private val journalpostPath = "/rest/journalpostapi/v1/journalpost"
    private val journalpostParamString = "forsoekFerdigstill"
    private val journalpostParamValue = true

    val url = UriComponentsBuilder.fromUriString(dokarkivUrl)
        .path(journalpostPath)
        .queryParam(journalpostParamString, journalpostParamValue)
        .toUriString()

    private val log = LoggerFactory.getLogger(DokarkivClient::class.qualifiedName)

    fun postDocumentsForsendelseToDokarkiv(
        fnr: String,
        forsendelseTittel: String,
        eksternReferanseId: String,
        documentsData: List<SingleDocumentData>,
        kanal: Distribusjonskanal?
    ): DokarkivResponse? {
        var upstream = "azuread"
        var operation = "get_system_token"
        return try {
            val token = azureAdClient.getSystemToken(dokarkivScope)

            val dokarkivRequest = createDokarkivRequestForDocuments(
                fnr,
                forsendelseTittel,
                eksternReferanseId,
                documentsData,
                kanal
            )

            upstream = "dokarkiv"
            operation = "create_journalpost"
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

                HttpStatus.UNAUTHORIZED -> {
                    logFailure("dokarkiv_request_failed", "Dokarkiv rejected authorization", status = 401)
                    null
                }

                else -> {
                    logFailure(
                        "dokarkiv_request_failed",
                        "Dokarkiv returned an unexpected status",
                        status = response.statusCode.value(),
                    )
                    null
                }
            }
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.CONFLICT) {
                handleConflict(e)
            } else {
                logFailure("dokarkiv_request_failed", "Document journaling request failed", e, upstream, operation)
                null
            }
        } catch (e: RestClientException) {
            logFailure("dokarkiv_request_failed", "Document journaling request failed", e, upstream, operation)
            null
        }
    }

    // Dokarkiv returns 409 when a journalpost already exists for the given eksternReferanseId,
    // which is idempotent behaviour, not an error. Treat it as success if it is actually ferdigstilt.
    private fun handleConflict(e: HttpClientErrorException): DokarkivResponse? {
        val dokarkivResponse = try {
            objectMapper.readValue(e.responseBodyAsString, DokarkivResponse::class.java)
        } catch (parseException: Exception) {
            logFailure(
                "dokarkiv_conflict_response_invalid",
                "Could not parse Dokarkiv conflict response",
                parseException,
                status = e.statusCode.value(),
            )
            return null
        }

        return if (dokarkivResponse != null && dokarkivResponse.journalpostferdigstilt == true) {
            log.info("Sending to dokarkiv successful, journalpost was already created and ferdigstilt")
            dokarkivResponse
        } else {
            logFailure(
                "dokarkiv_journalpost_not_finalized",
                "Dokarkiv conflict response did not confirm a finalized journalpost",
                status = e.statusCode.value(),
            )
            null
        }
    }

    private fun logFailure(
        event: String,
        message: String,
        failure: Exception? = null,
        upstream: String = "dokarkiv",
        operation: String = "create_journalpost",
        status: Int? = null,
    ) {
        log.error(
            appendEntries(
                buildMap {
                    put("event_type", event)
                    put("upstream", upstream)
                    put("operation", operation)
                    failure?.let { putAll(it.documentFailureFields()) }
                    status?.takeIf { it in 100..599 }?.let { put("upstream_status", it) }
                },
            ),
            message,
        )
    }

    fun postSingleDocumentToDokarkiv(
        fnr: String,
        pdf: ByteArray,
        eksternReferanseId: String,
        title: String,
        filnavn: String,
        kanal: Distribusjonskanal?
    ): DokarkivResponse? {
        val documentsData = listOf(
            SingleDocumentData(
                pdf = pdf,
                filnavn = filnavn,
                title = title,
            )
        )

        return postDocumentsForsendelseToDokarkiv(
            fnr,
            forsendelseTittel = title,
            eksternReferanseId,
            documentsData,
            kanal
        )
    }

    private fun createDokarkivRequestForDocuments(
        fnr: String,
        forsendelseTittel: String,
        eksternReferanseId: String,
        documentsData: List<SingleDocumentData>,
        kanal: Distribusjonskanal?
    ): DokarkivRequest {
        val dokumenter: List<Dokument> = documentsData.map {
            val dokumentvarianter = listOf(Dokumentvariant.create(fysiskDokument = it.pdf, filnavn = it.filnavn))

            Dokument.create(
                dokumentvarianter = dokumentvarianter,
                tittel = it.title,
            )
        }

        return DokarkivRequest.create(
            avsenderMottaker = AvsenderMottaker.create(fnr),
            dokumenter = dokumenter,
            eksternReferanseId = eksternReferanseId,
            tittel = forsendelseTittel,
            kanal = kanal,
        )
    }

    private fun createHttpEntity(exchangedToken: String, request: DokarkivRequest,): HttpEntity<*> {
        val headers = HttpHeaders()
        headers.add(HttpHeaders.AUTHORIZATION, bearerHeader(exchangedToken))
        headers.add(NAV_CALL_ID_HEADER, createCallId())
        headers.add(NAV_CONSUMER_ID_HEADER, MEROPPFOLGING_BACKEND_CONSUMER_ID)
        return HttpEntity<Any>(request, headers)
    }
}
