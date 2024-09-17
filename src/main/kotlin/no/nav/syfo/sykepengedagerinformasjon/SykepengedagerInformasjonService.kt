package no.nav.syfo.sykepengedagerinformasjon

import no.nav.syfo.logger
import org.springframework.stereotype.Service

@Service
class SykepengedagerInformasjonService {
    private val log = logger()

    fun processSykepengeDagerRecord(sykepengeDagerDTO: SykepengedagerInformasjonDTO) {
        log.info("[SYKEPENGEDAGER_INFO] Processing sykepengedager-inforasjon record with id: ${sykepengeDagerDTO.id}")
    }
}
