package no.nav.syfo.syfoopppdfgen

import no.nav.syfo.senoppfolging.v2.domain.BehovForOppfolgingSvar
import no.nav.syfo.senoppfolging.v2.domain.FremtidigSituasjonSvar
import no.nav.syfo.utils.formatDateForDisplayAndPdf
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

    fun createPdf(
        url: String,
        requestEntity: HttpEntity<PdfgenRequest>,
    ): ByteArray {
        try {
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

    fun createRequestEntity(
        brevdata: Brevdata,
    ): HttpEntity<PdfgenRequest> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.accept = mutableListOf(MediaType.APPLICATION_JSON)

        val body = PdfgenRequest(brevdata)
        return HttpEntity(body, headers)
    }

    fun createSenOppfolgingLandingPdf(
        daysLeft: String?,
        maxDateFormatted: String?,
        utbetaltTom: String?,
        isForReservertUser: Boolean = false,
    ): ByteArray {
        val url = if (isForReservertUser) pdfGenUrlSenOppfolingForReservedUsers else pdfGenUrlSenOppfolgingLanding

        val requestEntity =
            createSenOppfolgingLandingPdfRequestEntity(
                maxDateFormatted = maxDateFormatted,
                daysLeft = daysLeft,
                utbetaltTom = utbetaltTom,
            )

        return createPdf(url, requestEntity)
    }

    private fun createSenOppfolgingLandingPdfRequestEntity(
        daysLeft: String?,
        maxDateFormatted: String?,
        utbetaltTom: String?,
    ): HttpEntity<PdfgenRequest> {
        val brevDataSenOppfolgingLanding =
            BrevdataSenOppfolgingLanding(
                sendtdato = formatDateForDisplayAndPdf(LocalDate.now()),
                daysLeft = daysLeft,
                maxdato = maxDateFormatted,
                utbetaltTom = utbetaltTom,
            )

        return createRequestEntity(brevDataSenOppfolgingLanding)
    }

    fun createSenOppfolgingFormStepsPdf(
        fremtidigSituasjonSvar: FremtidigSituasjonSvar?,
        behovForOppfolgingSvar: BehovForOppfolgingSvar?,
    ): ByteArray {
        val url =
            "$pdfgenUrl/api/v1/genpdf/senoppfolging/form_steps"
        val requestEntity =
            createSenOppfolgingFormStepsPdfRequestEntity(
                fremtidigSituasjonSvar,
                behovForOppfolgingSvar,
            )

        return createPdf(url, requestEntity)
    }

    private fun createSenOppfolgingFormStepsPdfRequestEntity(
        fremtidigSituasjonSvar: FremtidigSituasjonSvar?,
        behovForOppfolgingSvar: BehovForOppfolgingSvar?,
    ): HttpEntity<PdfgenRequest> {
        val brevdataSenOppfolgingFormSteps =
            BrevdataSenOppfolgingFormSteps(
                fremtidigSituasjonAnswer = fremtidigSituasjonSvar,
                behovForOppfolgingAnswer = behovForOppfolgingSvar,
            )

        return createRequestEntity(brevdataSenOppfolgingFormSteps)
    }

    fun createSenOppfolgingReceiptPdf(
        behovForOppfolging: Boolean,
        questionTextFremtidigSituasjon: String?,
        answerTextFremtidigSituasjon: String?,
        questionTextBehovForOppfolging: String?,
        answerTextBehovForOppfolging: String?,
        submissionDateFormatted: String,
        maxDateFormatted: String?,
        utbetaltTomFormatted: String?,
        daysUntilMaxDate: String?,
    ): ByteArray {
        val url =
            "$pdfgenUrl/api/v1/genpdf/senoppfolging/receipt"
        val requestEntity =
            createSenOppfolgingReceiptPdfRequestEntity(
                behovForOppfolging = behovForOppfolging,
                questionTextFremtidigSituasjon = questionTextFremtidigSituasjon,
                answerTextFremtidigSituasjon = answerTextFremtidigSituasjon,
                questionTextBehovForOppfolging = questionTextBehovForOppfolging,
                answerTextBehovForOppfolging = answerTextBehovForOppfolging,
                submissionDateFormatted = submissionDateFormatted,
                maxDateFormatted = maxDateFormatted,
                utbetaltTomFormatted = utbetaltTomFormatted,
                daysUntilMaxDate = daysUntilMaxDate,
            )

        return createPdf(url, requestEntity)
    }

    private fun createSenOppfolgingReceiptPdfRequestEntity(
        behovForOppfolging: Boolean,
        questionTextFremtidigSituasjon: String?,
        answerTextFremtidigSituasjon: String?,
        questionTextBehovForOppfolging: String?,
        answerTextBehovForOppfolging: String?,
        submissionDateFormatted: String,
        maxDateFormatted: String?,
        utbetaltTomFormatted: String?,
        daysUntilMaxDate: String?,
    ): HttpEntity<PdfgenRequest> {
        val brevdataSenOppfolgingReceipt =
            BrevdataSenOppfolgingReceipt(
                behovForOppfolging = behovForOppfolging,
                questionTextFremtidigSituasjon = questionTextFremtidigSituasjon,
                answerTextFremtidigSituasjon = answerTextFremtidigSituasjon,
                questionTextBehovForOppfolging = questionTextBehovForOppfolging,
                answerTextBehovForOppfolging = answerTextBehovForOppfolging,
                submissionDateFormatted = submissionDateFormatted,
                maxdatoFormatted = maxDateFormatted,
                utbetaltTomFormatted = utbetaltTomFormatted,
                daysUntilMaxDate = daysUntilMaxDate,
            )

        return createRequestEntity(brevdataSenOppfolgingReceipt)
    }
}
