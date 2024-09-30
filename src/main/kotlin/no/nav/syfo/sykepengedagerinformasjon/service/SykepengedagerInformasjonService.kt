package no.nav.syfo.sykepengedagerinformasjon.service

import no.nav.syfo.logger
import no.nav.syfo.sykepengedagerinformasjon.database.SykepengedagerInformasjonDAO
import no.nav.syfo.sykepengedagerinformasjon.domain.SykepengedagerInformasjonDTO
import org.springframework.stereotype.Service

@Service
class SykepengedagerInformasjonService(private val sykepengedagerInformasjonDAO: SykepengedagerInformasjonDAO) {
    private val log = logger()

    fun processSykepengeDagerRecord(sykepengeDagerDTO: SykepengedagerInformasjonDTO) {
        log.info("[SYKEPENGEDAGER_INFO] Persisting sykepengedager-inforasjon record with id: ${sykepengeDagerDTO.id}")
        sykepengedagerInformasjonDAO.persistSykepengedagerInformasjon(sykepengeDagerDTO)
    }
}
