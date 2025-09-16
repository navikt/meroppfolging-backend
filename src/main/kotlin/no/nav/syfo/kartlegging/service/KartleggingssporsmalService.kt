package no.nav.syfo.kartlegging.service

import no.nav.syfo.kartlegging.database.KartleggingssporsmalDAO
import no.nav.syfo.kartlegging.domain.Kartleggingssporsmal
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalRequest
import org.springframework.stereotype.Service

@Service
class KartleggingssporsmalService(
    private val kartleggingssporsmalDAO: KartleggingssporsmalDAO,
) {

    fun persistKartleggingssporsmal(personIdent: String, kartleggingssporsmalRequest: KartleggingssporsmalRequest) {
        val kartleggingssporsmal = Kartleggingssporsmal(
            fnr = personIdent,
            formSnapshot = kartleggingssporsmalRequest.formSnapshot
        )
        kartleggingssporsmalDAO.persistKartleggingssporsmal(kartleggingssporsmal)
    }
}
