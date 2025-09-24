package no.nav.syfo.kartlegging.kafka

import java.time.Instant
import java.util.UUID

data class KartleggingssvarEvent(
    val personident: String,
    val kandidatId: UUID,
    val svarId: UUID,
    val svarAt: Instant,
)
