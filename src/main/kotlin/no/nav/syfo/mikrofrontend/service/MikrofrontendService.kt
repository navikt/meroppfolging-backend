package no.nav.syfo.mikrofrontend.service

import no.nav.syfo.kartlegging.service.KandidatService
import no.nav.syfo.kartlegging.service.KartleggingssporsmalService
import no.nav.syfo.mikrofrontend.domain.MerOppfolgingStatusDTO
import no.nav.syfo.mikrofrontend.domain.toKartleggingStatusDTO
import no.nav.syfo.senoppfolging.service.SenOppfolgingService
import org.springframework.stereotype.Service

@Service
class MikrofrontendService(
    private val senOppfolgingService: SenOppfolgingService,
    private val kartleggingssporsmalService: KartleggingssporsmalService,
    private val kandidatService: KandidatService,
) {
    fun status(personIdent: String, token: String): MerOppfolgingStatusDTO {
        val senOppfolgingStatus =
            senOppfolgingService.prepareAndBuildSenOppfolgingStatusDTOV2(personIdent, token)
        if (senOppfolgingStatus.hasAccessToSenOppfolging) {
            return MerOppfolgingStatusDTO.SenOppfolging(senOppfolgingStatus)
        }

        val kandidat = kandidatService.getKandidatByFnr(personIdent)
        if (kandidat?.isKandidat() == true) {
            val latestResponse =
                kartleggingssporsmalService.getLatestKartleggingssporsmal(kandidat.kandidatId)
            val kartleggingStatus = latestResponse.toKartleggingStatusDTO(hasAccessToKartlegging = true)
            return MerOppfolgingStatusDTO.Kartlegging(kartleggingStatus)
        }

        return MerOppfolgingStatusDTO.IngenOppfolging()
    }
}
