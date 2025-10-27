package no.nav.syfo.kartlegging.service

import no.nav.syfo.kartlegging.database.KandidatDAO
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalKandidat
import no.nav.syfo.logger
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.collections.forEach

@Service
class KandidatService(
    private val kandidatDAO: KandidatDAO,
) {
    private val logger = logger()

    fun getKandidatByFnr(fnr: String): KartleggingssporsmalKandidat? {
        return kandidatDAO.findKandidatByFnr(fnr)
    }

    fun getKandidatByKandidatId(kandidatId: UUID): KartleggingssporsmalKandidat? {
        return kandidatDAO.findKandidatByKandidatId(kandidatId)
    }

    @Transactional
    fun persistKandidater(kandidater: List<KartleggingssporsmalKandidat>) {
        kandidater.forEach { kandidat ->
            try {
                kandidatDAO.persistKandidat(kandidat)
            } catch (e: DuplicateKeyException) {
                logger.error("Kandidat with kandidatId: ${kandidat.kandidatId} already exists. Skipping...", e)
            }
        }
    }
}
