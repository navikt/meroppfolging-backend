package no.nav.syfo.sykepengedagerinformasjon.domain

import no.nav.syfo.utils.formatDateForLetter
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

fun PSykepengedagerInformasjon.forelopigBeregnetSluttFormattedForLetter(): String {
    return formatDateForLetter(this.forelopigBeregnetSlutt)
}

fun PSykepengedagerInformasjon.utbetaltTomFormattedForLetter(): String {
    return formatDateForLetter(this.utbetaltTom)
}
