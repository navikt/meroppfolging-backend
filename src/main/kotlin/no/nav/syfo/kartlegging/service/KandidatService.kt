package no.nav.syfo.kartlegging.service

import no.nav.syfo.kartlegging.database.KandidatDAO
import no.nav.syfo.kartlegging.kafka.KandidatStatus
import org.springframework.stereotype.Service

@Service
class KandidatService(
    private val kandidatDAO: KandidatDAO,
) {
    fun isSykmeldtKandidat(fnr: String): Boolean {
        val muligKandidat = kandidatDAO.findKandidatByFnr(fnr)
        return muligKandidat?.status == KandidatStatus.KANDIDAT
    }

}
