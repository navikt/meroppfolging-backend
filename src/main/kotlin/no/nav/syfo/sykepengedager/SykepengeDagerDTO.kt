package no.nav.syfo.sykepengedager

import java.time.LocalDate
import java.time.LocalDateTime

data class SykepengeDagerDTO(
    val id: String,
    val fnr: String,
    val maksDato: LocalDate,
    val utbetaltTom: LocalDate,
    val gjenstaendeSykedager: Int,
    val opprettet: LocalDateTime,
)
