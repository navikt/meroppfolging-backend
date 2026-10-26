package no.nav.syfo.pdfgen

import net.logstash.logback.marker.Markers.appendEntries
import no.nav.syfo.documentFailureFields
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
import org.springframework.web.client.exchange
import java.time.LocalDate

@Component
class PdfgenClient(
    @Value("\${ESYFO_PDFGEN_URL}")
    esyfoPdfgenUrl: String,
    private val restTemplate: RestTemplate,
) {
    private val log = LoggerFactory.getLogger(PdfgenClient::class.java)

    private val esyfoPdfgen = "$esyfoPdfgenUrl/api/v1/genpdf"

    private val senOppfolgingLandingUrl = "$esyfoPdfgen/senoppfolging/landing"
    private val senOppfolgingSnartSluttPaaSykepengerUrl =
        "$esyfoPdfgen/senoppfolging/snart_slutt_paa_sykepenger_reservert"
    private val senOppfolgingFormStepsUrl = "$esyfoPdfgen/senoppfolging/form_steps"
    private val senOppfolgingReceiptUrl = "$esyfoPdfgen/senoppfolging/receipt"
    private val kartleggingReceiptUrl = "$esyfoPdfgen/kartlegging/receipt"

    fun createSenOppfolgingLandingPdf(
        daysLeft: String?,
        maxDateFormatted: String?,
        utbetaltTom: String?,
        isForReservertUser: Boolean = false,
    ): ByteArray {
        val url = if (isForReservertUser) senOppfolgingSnartSluttPaaSykepengerUrl else senOppfolgingLandingUrl
        val brevdata = BrevdataSenOppfolgingLanding(
            sendtdato = formatDateForDisplayAndPdf(LocalDate.now()),
            daysLeft = daysLeft,
            maxdato = maxDateFormatted,
            utbetaltTom = utbetaltTom,
        )
        return createPdf(url, brevdata)
    }

    fun createSenOppfolgingFormStepsPdf(
        fremtidigSituasjonSvar: FremtidigSituasjonSvar?,
        behovForOppfolgingSvar: BehovForOppfolgingSvar?,
    ): ByteArray {
        val brevdata = BrevdataSenOppfolgingFormSteps(
            fremtidigSituasjonAnswer = fremtidigSituasjonSvar,
            behovForOppfolgingAnswer = behovForOppfolgingSvar,
        )
        return createPdf(senOppfolgingFormStepsUrl, brevdata)
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
        val brevdata = BrevdataSenOppfolgingReceipt(
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
        return createPdf(senOppfolgingReceiptUrl, brevdata)
    }

    fun getKartleggingPdf(kartleggingPdfgenRequest: KartleggingPdfgenRequest,): ByteArray =
        createPdf(kartleggingReceiptUrl, kartleggingPdfgenRequest)

    private fun createPdf(url: String, brevdata: Brevdata): ByteArray {
        try {
            return restTemplate
                .exchange<ByteArray>(
                    url,
                    HttpMethod.POST,
                    createRequestEntity(brevdata),
                ).body!!
        } catch (e: RestClientResponseException) {
            log.error(
                appendEntries(
                    e.documentFailureFields() + mapOf(
                        "event_type" to "pdf_generation_failed",
                        "upstream" to "esyfo-pdfgen",
                    ),
                ),
                "PDF generation failed",
            )
            throw e
        }
    }

    private fun createRequestEntity(brevdata: Brevdata): HttpEntity<PdfgenRequest> {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            accept = listOf(MediaType.APPLICATION_JSON)
        }
        return HttpEntity(PdfgenRequest(brevdata), headers)
    }
}
