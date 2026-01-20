package no.nav.syfo.sykmelding.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class ReceivedSykmeldingDTO(
    val sykmelding: Sykmelding,
    val personNrPasient: String,
    val mottattDato: LocalDateTime,
)

data class Sykmelding(val id: String, val perioder: List<Periode>,)

data class Periode(val fom: LocalDate, val tom: LocalDate,)
