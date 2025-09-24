package no.nav.syfo.kartlegging.kafka

import no.nav.syfo.kartlegging.domain.KandidatStatus
import java.time.Instant
import java.util.UUID

data class KandidatEvent(
    val personIdent: String,
    val kandidatId: UUID,
    val status: KandidatStatus,
    val createdAt: Instant
)
