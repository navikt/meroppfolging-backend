package no.nav.syfo.kartlegging.kafka

import no.nav.syfo.kartlegging.domain.KandidatStatus
import java.time.OffsetDateTime
import java.util.UUID

data class KandidatKafkaEvent(
    val personIdent: String,
    val kandidatId: UUID,
    val status: KandidatStatus,
    val createdAt: OffsetDateTime,
)
