package no.nav.syfo.senoppfolging.kafka

import java.time.LocalDateTime
import java.util.UUID

data class KSenOppfolgingVarselDTO(
    val uuid: UUID,
    val fnr: String,
    val createdAt: LocalDateTime,
)
