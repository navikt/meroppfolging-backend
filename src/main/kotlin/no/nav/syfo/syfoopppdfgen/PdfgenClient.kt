package no.nav.syfo.syfoopppdfgen

import no.nav.syfo.utils.formatDateForLetter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import java.time.LocalDate

@Component
class PdfgenClient(
    @Value("\${SYFOOPPDFGEN_URL}")
    private val pdfgenUrl: String,
    private val restTemplate: RestTemplate,
) {
    private val log = LoggerFactory.getLogger(PdfgenClient::class.java)

    fun getMerVeiledningPdf(
        pdfEndpoint: String,
        utbetaltTom: String?,
        maxDate: String?,

    ): ByteArray {
        try {
            val requestEntity =
                getMerVeiledningPdfRequestEntity(
                    utbetaltTom = utbetaltTom,
                    maxDate = maxDate,
                )
            return restTemplate
                .exchange(
                    "$pdfgenUrl/api/v1/genpdf$pdfEndpoint",
                    HttpMethod.POST,
                    requestEntity,
                    ByteArray::class.java,
                ).body!!
        } catch (e: RestClientResponseException) {
            log.error(
                "Call to get PDF from pdfgen failed " +
                    "with status: ${e.statusCode} and message: ${e.responseBodyAsString}",
                e,
            )
            throw e
        }
    }

    private fun getMerVeiledningPdfRequestEntity(
        utbetaltTom: String?,
        maxDate: String?,
    ): HttpEntity<PdfgenRequest> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = mutableListOf(MediaType.APPLICATION_JSON)

        val body = PdfgenRequest(
            BrevdataMerVeiledning(
                sendtdato = formatDateForLetter(LocalDate.now()),
                utbetaltTom = utbetaltTom,
                maxdato = maxDate,
            ),
        )
        return HttpEntity(body, headers)
    }

    fun getMerVeiledningDigitalUserPdf(
        pdfEndpoint: String,
        daysLeft: String?,
        maxDate: String?,
    ): ByteArray {
        try {
            val requestEntity =
                getMerVeiledningDigitalUserPdfRequestEntity(
                    maxDate = maxDate,
                    daysLeft = daysLeft,
                )
            return restTemplate
                .exchange(
                    "$pdfgenUrl/api/v1/genpdf$pdfEndpoint",
                    HttpMethod.POST,
                    requestEntity,
                    ByteArray::class.java,
                ).body!!
        } catch (e: RestClientResponseException) {
            log.error(
                "Call to get PDF from pdfgen failed " +
                    "with status: ${e.statusCode} and message: ${e.responseBodyAsString}",
                e,
            )
            throw e
        }
    }

    private fun getMerVeiledningDigitalUserPdfRequestEntity(
        daysLeft: String?,
        maxDate: String?,
    ): HttpEntity<PdfgenRequest> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = mutableListOf(MediaType.APPLICATION_JSON)

        val body = PdfgenRequest(
            BrevdataMerVeiledningPilot(
                sendtdato = formatDateForLetter(LocalDate.now()),
                daysLeft = daysLeft,
                maxdato = maxDate,
            ),
        )
        return HttpEntity(body, headers)
    }

    fun getSenOppfolgingPdf(
        kvitteringEndpoint: String,
        behovForOppfolging: Boolean,
        daysUntilMaxDate: String?,
    ): ByteArray {
        try {
            val requestEntity =
                getSenOppfolgingPdfRequestEntity(
                    daysUntilMaxDate = daysUntilMaxDate,
                    behovForOppfolging = behovForOppfolging,
                )
            return restTemplate
                .exchange(
                    "$pdfgenUrl/api/v1/genpdf/senoppfolging/$kvitteringEndpoint",
                    HttpMethod.POST,
                    requestEntity,
                    ByteArray::class.java,
                ).body!!
        } catch (e: RestClientResponseException) {
            log.error(
                "Call to get AzureADToken from pdfgen failed " +
                    "with status: ${e.statusCode} and message: ${e.responseBodyAsString}",
                e,
            )
            throw e
        }
    }

    private fun getSenOppfolgingPdfRequestEntity(
        daysUntilMaxDate: String?,
        behovForOppfolging: Boolean,
    ): HttpEntity<PdfgenRequest> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = mutableListOf(MediaType.APPLICATION_JSON)

        val body =
            PdfgenRequest(
                brevdata =
                BrevdataSenOppfolging(
                    daysUntilMaxDate = daysUntilMaxDate,
                    behovForOppfolging = behovForOppfolging,
                    sentDate = formatDateForLetter(LocalDate.now()),
                ),
            )
        return HttpEntity(body, headers)
    }
}
