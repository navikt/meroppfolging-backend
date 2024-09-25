package no.nav.syfo.sykmelding.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class PSykmelding(
    val sykmeldingId: String,
    val employeeIdentificationNumber: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val createdAt: LocalDateTime,
)
