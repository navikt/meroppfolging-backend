package no.nav.syfo.syfoopppdfgen

import no.nav.syfo.utils.formatDateForDisplay
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

    private val pdfGenUrlSenOppfolingForReservedUsers =
        "$pdfgenUrl/api/v1/genpdf/oppfolging/mer_veiledning_for_reserverte"
    private val pdfGenUrlSenOppfolgingLanding = "$pdfgenUrl/api/v1/genpdf/senoppfolging/landing"

    fun createSenOppfolgingLandingPdf(
        daysLeft: String?,
        maxDate: String?,
        utbetaltTom: String?,
        isForReservertUser: Boolean = false,
    ): ByteArray {
        try {
            val url = if (isForReservertUser) pdfGenUrlSenOppfolingForReservedUsers else pdfGenUrlSenOppfolgingLanding

            val requestEntity =
                createSenOppfolgingLandingPdfRequestEntity(
                    maxDate = maxDate,
                    daysLeft = daysLeft,
                    utbetaltTom = utbetaltTom,
                )

            return restTemplate
                .exchange(
                    url,
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

    private fun createSenOppfolgingLandingPdfRequestEntity(
        daysLeft: String?,
        maxDate: String?,
        utbetaltTom: String?,
    ): HttpEntity<PdfgenRequest> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = mutableListOf(MediaType.APPLICATION_JSON)

        val body = PdfgenRequest(
            BrevdataSenOppfolgingLanding(
                sendtdato = formatDateForDisplay(LocalDate.now()),
                daysLeft = daysLeft,
                maxdato = maxDate,
                utbetaltTom = utbetaltTom,
            ),
        )
        return HttpEntity(body, headers)
    }

    fun createSenOppfolgingReceiptPdf(
        behovForOppfolging: Boolean,
        questionTextFremtidigSituasjon: String?,
        answerTextFremtidigSituasjon: String?,
        questionTextBehovForOppfolging: String?,
        answerTextBehovForOppfolging: String?,
        submittedDateFormatted: String,
        maxDate: String?,
        daysUntilMaxDate: String?,
    ): ByteArray {
        val url =
            "$pdfgenUrl/api/v1/genpdf/senoppfolging/receipt"
        try {
            val requestEntity =
                createSenOppfolgingReceiptPdfRequestEntity(
                    behovForOppfolging = behovForOppfolging,
                    questionTextFremtidigSituasjon = questionTextFremtidigSituasjon,
                    answerTextFremtidigSituasjon = answerTextFremtidigSituasjon,
                    questionTextBehovForOppfolging = questionTextBehovForOppfolging,
                    answerTextBehovForOppfolging = answerTextBehovForOppfolging,
                    submittedDateFormatted = submittedDateFormatted,
                    maxDate = maxDate,
                    daysUntilMaxDate = daysUntilMaxDate,
                )
            return restTemplate
                .exchange(
                    url,
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

    private fun createSenOppfolgingReceiptPdfRequestEntity(
        behovForOppfolging: Boolean,
        questionTextFremtidigSituasjon: String?,
        answerTextFremtidigSituasjon: String?,
        questionTextBehovForOppfolging: String?,
        answerTextBehovForOppfolging: String?,
        submittedDateFormatted: String,
        maxDate: String?,
        daysUntilMaxDate: String?,
    ): HttpEntity<PdfgenRequest> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = mutableListOf(MediaType.APPLICATION_JSON)

        val body =
            PdfgenRequest(
                brevdata =
                BrevdataSenOppfolgingReceipt(
                    sentDate = formatDateForDisplay(LocalDate.now()),
                    behovForOppfolging = behovForOppfolging,
                    questionTextFremtidigSituasjon = questionTextFremtidigSituasjon,
                    answerTextFremtidigSituasjon = answerTextFremtidigSituasjon,
                    questionTextBehovForOppfolging = questionTextBehovForOppfolging,
                    answerTextBehovForOppfolging = answerTextBehovForOppfolging,
                    submittedDateFormatted = submittedDateFormatted,
                    maxdato = maxDate,
                    daysUntilMaxDate = daysUntilMaxDate,
                ),
            )
        return HttpEntity(body, headers)
    }
}
