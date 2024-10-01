package no.nav.syfo.sykepengedagerinformasjon.domain

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
