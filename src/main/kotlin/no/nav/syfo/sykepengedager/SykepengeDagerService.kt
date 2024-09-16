package no.nav.syfo.sykepengedager

import org.springframework.stereotype.Service

@Service
class SykepengeDagerService(
    private val sykepengeDagerDAO: SykepengeDagerDAO,
) {
    fun processSykepengeDagerRecord(sykepengeDagerDTO: SykepengeDagerDTO) {
        sykepengeDagerDAO.storeSykepengeDager(
            fnr = sykepengeDagerDTO.fnr,
            maksDato = sykepengeDagerDTO.maksDato,
            utbetaltTom = sykepengeDagerDTO.utbetaltTom,
            gjenstaendeSykedager = sykepengeDagerDTO.gjenstaendeSykedager,
            sykepengedagerId = sykepengeDagerDTO.id,
        )
    }
}
