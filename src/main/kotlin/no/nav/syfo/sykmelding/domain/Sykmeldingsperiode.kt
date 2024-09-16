package no.nav.syfo.sykmelding.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class Sykmeldingsperiode(
    val sykmeldingId: String,
    val hasEmployer: Boolean,
    val employeeIdentificationNumber: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val createdAt: LocalDateTime,
)
