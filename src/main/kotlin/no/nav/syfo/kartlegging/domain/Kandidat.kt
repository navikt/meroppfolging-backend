package no.nav.syfo.kartlegging.domain

import no.nav.syfo.kartlegging.kafka.KandidatStatus
import java.time.Instant

data class Kandidat(
    val personIdent: String,
    val kandidatId: String,
    val status: KandidatStatus,
    val createdAt: Instant
)
