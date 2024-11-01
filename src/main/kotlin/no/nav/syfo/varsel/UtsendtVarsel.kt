package no.nav.syfo.varsel

import java.time.LocalDateTime
import java.util.*

interface Varsel {
    val uuid: UUID
    val personIdent: String
    val utsendtTidspunkt: LocalDateTime
}

data class UtsendtVarsel(
    override val uuid: UUID,
    override val personIdent: String,
    override val utsendtTidspunkt: LocalDateTime,
    val utbetalingId: String,
    val sykmeldingId: String,
) : Varsel

data class UtsendtVarselEsyfovarselCopy(
    override val uuid: UUID,
    override val personIdent: String,
    override val utsendtTidspunkt: LocalDateTime,
) : Varsel
