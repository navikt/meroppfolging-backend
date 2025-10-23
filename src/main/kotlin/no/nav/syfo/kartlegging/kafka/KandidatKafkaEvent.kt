package no.nav.syfo.kartlegging.kafka

import java.time.OffsetDateTime
import java.util.UUID

data class KandidatKafkaEvent(
    val personident: String,
    val kandidatUuid: UUID,
    val status: String,
    val createdAt: OffsetDateTime,
)
