package no.nav.syfo.kartlegging.kafka

import java.time.Instant

enum class KandidatStatus {
    KANDIDAT,
    IKKE_KANDIDAT,
}

data class KandidatEvent(
    val personIdent: String,
    val kandidatId: String,
    val status: KandidatStatus,
    val createdAt: Instant
)
