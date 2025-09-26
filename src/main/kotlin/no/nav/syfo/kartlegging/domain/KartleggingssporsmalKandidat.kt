package no.nav.syfo.kartlegging.domain

import java.time.Instant
import java.util.UUID

enum class KandidatStatus {
    KANDIDAT,
    IKKE_KANDIDAT,
}

data class KartleggingssporsmalKandidat(
    val personIdent: String,
    val kandidatId: UUID,
    val status: KandidatStatus,
    val createdAt: Instant
) {
    fun isKandidat(): Boolean = status == KandidatStatus.KANDIDAT
}
