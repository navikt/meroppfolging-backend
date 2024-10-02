package no.nav.syfo.varsel

import java.time.LocalDateTime
import java.util.*

data class UtsendtVarsel(
    val uuid: UUID,
    val personIdent: String,
    val utsendtTidspunkt: LocalDateTime,
    val utbetalingId: String,
    val sykmeldingId: String,
)
