package no.nav.syfo.syfoopppdfgen

import no.nav.syfo.senoppfolging.v2.domain.FremtidigSituasjonSvar
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

    private val pdfGenUrlSenOppfolingForReservedUsers =
        "$pdfgenUrl/api/v1/genpdf/oppfolging/mer_veiledning_for_reserverte"
    private val pdfGenUrlSenOppfolgingLanding = "$pdfgenUrl/api/v1/genpdf/senoppfolging/landing"

    private fun getSenOppfolgingKvitteringEndpoint(fremtidigSituasjonSvar: FremtidigSituasjonSvar): String {
        return when (fremtidigSituasjonSvar) {
            FremtidigSituasjonSvar.USIKKER -> "usikker_receipt"
            FremtidigSituasjonSvar.BYTTE_JOBB -> "bytte_jobb_receipt"
            FremtidigSituasjonSvar.FORTSATT_SYK -> "fortsatt_syk_receipt"
            FremtidigSituasjonSvar.TILBAKE_GRADERT -> "tilbake_gradert_receipt"
            FremtidigSituasjonSvar.TILBAKE_MED_TILPASNINGER -> "tilbake_med_tilpasninger_receipt"
            FremtidigSituasjonSvar.TILBAKE_HOS_ARBEIDSGIVER -> "tilbake_hos_arbeidsgiver_receipt"
            else -> {
                log.error("Could not map FremtidigSituasjonSvar type: $fremtidigSituasjonSvar")
                throw IllegalArgumentException("Invalid FremtidigSituasjonSvar type: $fremtidigSituasjonSvar")
            }
        }
    }

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
                sendtdato = formatDateForLetter(LocalDate.now()),
                daysLeft = daysLeft,
                maxdato = maxDate,
                utbetaltTom = utbetaltTom,
            ),
        )
        return HttpEntity(body, headers)
    }

    fun createSenOppfolgingReceiptPdf(
        fremtidigSituasjonSvar: FremtidigSituasjonSvar,
        behovForOppfolging: Boolean,
        maxDate: String?,
        daysUntilMaxDate: String?,
    ): ByteArray {
        val url =
            "$pdfgenUrl/api/v1/genpdf/senoppfolging/${getSenOppfolgingKvitteringEndpoint(fremtidigSituasjonSvar)}"
        try {
            val requestEntity =
                createSenOppfolgingReceiptPdfRequestEntity(
                    behovForOppfolging = behovForOppfolging,
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
                    sentDate = formatDateForLetter(LocalDate.now()),
                    behovForOppfolging = behovForOppfolging,
                    maxdato = maxDate,
                    daysUntilMaxDate = daysUntilMaxDate,
                ),
            )
        return HttpEntity(body, headers)
    }
}
