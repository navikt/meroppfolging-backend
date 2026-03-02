package no.nav.syfo.kartlegging.domain

import no.nav.syfo.kartlegging.domain.formsnapshot.FormSnapshot
import java.time.Instant
import java.util.UUID

data class KartleggingssporsmalRequest(val formSnapshot: FormSnapshot,)

open class Kartleggingssporsmal(open val fnr: String, open val kandidatId: UUID, open val formSnapshot: FormSnapshot,)

data class PersistedKartleggingssporsmal(
    val uuid: UUID,
    override val fnr: String,
    override val kandidatId: UUID,
    override val formSnapshot: FormSnapshot,
    val createdAt: Instant,
) : Kartleggingssporsmal(
    fnr = fnr,
    kandidatId = kandidatId,
    formSnapshot = formSnapshot,
)

data class KandidatStatusResponse(val isKandidat: Boolean, val formResponse: PersistedKartleggingssporsmal?)
