package no.nav.syfo.kartlegging.service

import no.nav.syfo.kartlegging.database.KandidatDAO
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalKandidat
import org.springframework.stereotype.Service

@Service
class KandidatService(
    private val kandidatDAO: KandidatDAO,
) {
    fun getKandidatByFnr(fnr: String): KartleggingssporsmalKandidat? {
        return kandidatDAO.findKandidatByFnr(fnr)
    }
}
