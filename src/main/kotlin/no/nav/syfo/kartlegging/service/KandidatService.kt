package no.nav.syfo.kartlegging.service

import no.nav.syfo.kartlegging.database.KandidatDAO
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalKandidat
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.collections.forEach

@Service
class KandidatService(
    private val kandidatDAO: KandidatDAO,
) {
    fun getKandidatByFnr(fnr: String): KartleggingssporsmalKandidat? {
        return kandidatDAO.findKandidatByFnr(fnr)
    }

    @Transactional
    fun persistKandidater(kandidater: List<KartleggingssporsmalKandidat>) {
        kandidater.forEach { kandidat ->
            kandidatDAO.persistKandidat(kandidat)
        }
    }
}
