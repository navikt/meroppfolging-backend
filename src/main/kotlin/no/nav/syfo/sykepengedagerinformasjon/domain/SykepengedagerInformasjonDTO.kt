package no.nav.syfo.sykepengedagerinformasjon.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class SykepengedagerInformasjonDTO(
    val id: String,
    val personIdent: String,
    val forelopigBeregnetSlutt: LocalDate,
    val utbetaltTom: LocalDate,
    val gjenstaendeSykedager: String,
    val createdAt: LocalDateTime,
)
