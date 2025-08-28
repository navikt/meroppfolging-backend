package no.nav.syfo.sykepengedagerinformasjon.domain

import no.nav.syfo.utils.formatDateForDisplayAndPdf
import java.time.LocalDate
import java.time.LocalDateTime

data class PSykepengedagerInformasjon(
    val utbetalingId: String,
    val personIdent: String,
    val forelopigBeregnetSlutt: LocalDate,
    val utbetaltTom: LocalDate,
    val gjenstaendeSykedager: Int,
    val utbetalingCreatedAt: LocalDateTime,
    val receivedAt: LocalDateTime,
)

fun PSykepengedagerInformasjon.forelopigBeregnetSluttFormatted(): String {
    return formatDateForDisplayAndPdf(this.forelopigBeregnetSlutt)
}

fun PSykepengedagerInformasjon.utbetaltTomFormatted(): String {
    return formatDateForDisplayAndPdf(this.utbetaltTom)
}
