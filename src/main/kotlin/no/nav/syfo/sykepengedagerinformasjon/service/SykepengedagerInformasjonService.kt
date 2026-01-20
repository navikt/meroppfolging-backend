package no.nav.syfo.sykepengedagerinformasjon.service

import no.nav.syfo.logger
import no.nav.syfo.sykepengedagerinformasjon.database.SykepengedagerInformasjonDAO
import no.nav.syfo.sykepengedagerinformasjon.domain.PSykepengedagerInformasjon
import no.nav.syfo.sykepengedagerinformasjon.domain.SykepengedagerInformasjonDTO
import org.postgresql.util.PSQLException
import org.springframework.stereotype.Service

@Service
@Suppress("SwallowedException")
class SykepengedagerInformasjonService(private val sykepengedagerInformasjonDAO: SykepengedagerInformasjonDAO) {
    private val log = logger()

    fun processSykepengeDagerRecord(sykepengeDagerDTO: SykepengedagerInformasjonDTO) {
        log.info("[SYKEPENGEDAGER_INFO] Persisting sykepengedager-inforasjon record with id: ${sykepengeDagerDTO.id}")
        try {
            sykepengedagerInformasjonDAO.persistSykepengedagerInformasjon(sykepengeDagerDTO)
        } catch (e: PSQLException) {
            log.error("Unable to persist record from SPDI with id: ${sykepengeDagerDTO.id} due to PSQLException")
        }
    }

    fun fetchSykepengedagerInformasjonByIdent(personIdent: String): PSykepengedagerInformasjon? =
        sykepengedagerInformasjonDAO.fetchSykepengedagerInformasjonByIdent(personIdent)
}
