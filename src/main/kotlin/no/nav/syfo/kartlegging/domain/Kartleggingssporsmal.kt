package no.nav.syfo.kartlegging.domain

import no.nav.syfo.kartlegging.domain.formsnapshot.FormSnapshot
import java.time.Instant
import java.util.UUID

data class KartleggingssporsmalRequest(
    val formSnapshot: FormSnapshot,
)


data class Kartleggingssporsmal (
    val fnr: String,
    val formSnapshot: FormSnapshot,
)

data class PersistedKartleggingssporsmal (
    val uuid: UUID,
    val fnr: String,
    val formSnapshot: FormSnapshot,
    val createdAt: Instant,
)
