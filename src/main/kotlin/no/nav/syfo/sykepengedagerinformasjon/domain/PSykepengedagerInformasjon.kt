package no.nav.syfo.sykepengedagerinformasjon.domain

import no.nav.syfo.utils.formatDateForDisplay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    return formatDateForDisplay(this.forelopigBeregnetSlutt)
}

fun PSykepengedagerInformasjon.forelopigBeregnetSluttISO(): String {
    return this.forelopigBeregnetSlutt.format(DateTimeFormatter.ISO_LOCAL_DATE)
}

fun PSykepengedagerInformasjon.utbetaltTomFormatted(): String {
    return formatDateForDisplay(this.utbetaltTom)
}

fun PSykepengedagerInformasjon.utbetaltTomISO(): String {
    return this.utbetaltTom.format(DateTimeFormatter.ISO_LOCAL_DATE)
}
