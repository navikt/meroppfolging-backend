package no.nav.syfo.dokarkiv

import SingleDocumentData
import no.nav.syfo.MEROPPFOLGING_BACKEND_CONSUMER_ID
import no.nav.syfo.NAV_CALL_ID_HEADER
import no.nav.syfo.NAV_CONSUMER_ID_HEADER
import no.nav.syfo.auth.azuread.AzureAdClient
import no.nav.syfo.auth.bearerHeader
import no.nav.syfo.createCallId
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

    fun postDocumentsForsendelseToDokarkiv(
        fnr: String,
        forsendelseTittel: String,
        eksternReferanseId: String,
        documentsData: List<SingleDocumentData>,
        kanal: Distribusjonskanal?
    ): DokarkivResponse? {
        return try {
            val token = azureAdClient.getSystemToken(dokarkivScope)

            val dokarkivRequest = createDokarkivRequestForDocuments(
                fnr,
                forsendelseTittel,
                eksternReferanseId,
                documentsData,
                kanal
            )

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
