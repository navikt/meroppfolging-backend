package no.nav.syfo.varsel

import java.time.LocalDateTime
import java.util.*

data class UtsendtVarsel(
    val uuid: UUID,
    val fnr: String,
    val utsendtTidspunkt: LocalDateTime,
    val sykepengedagerId: String,
)
