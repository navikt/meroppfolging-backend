package no.nav.syfo.kartlegging.kafka

import java.time.OffsetDateTime
import java.util.UUID

data class KartleggingssvarEvent(
    val personident: String,
    val svarId: UUID,
    val svarAt: OffsetDateTime
)
