package no.nav.syfo.kartlegging.domain

import java.time.Duration
import java.time.Instant
import java.util.UUID

private const val KANDIDAT_VALID_DAYS: Long = 31

enum class KandidatStatus {
    KANDIDAT,
    IKKE_KANDIDAT,
}

enum class Skjemavariant {
    FLERVALG_V1,
    FLERVALG_FRITEKST_V1
}

data class KartleggingssporsmalKandidat(
    val personIdent: String,
    val kandidatId: UUID,
    val status: KandidatStatus,
    val skjemavariant: Skjemavariant,
    val createdAt: Instant
) {
    fun isKandidat(): Boolean {
        if (status != KandidatStatus.KANDIDAT) return false
        val latestBoundary = Instant.now().minus(Duration.ofDays(KANDIDAT_VALID_DAYS))
        return createdAt.isAfter(latestBoundary)
    }
}
