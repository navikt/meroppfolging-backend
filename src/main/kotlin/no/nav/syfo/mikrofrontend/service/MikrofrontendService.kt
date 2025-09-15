package no.nav.syfo.mikrofrontend.service

import no.nav.syfo.mikrofrontend.domain.MerOppfolgingStatusDTO
import no.nav.syfo.senoppfolging.service.SenOppfolgingService
import org.springframework.stereotype.Service

@Service
class MikrofrontendService(
    private val senOppfolgingService: SenOppfolgingService,
    // private val kartleggingService: KartleggingService
) {
    fun status(personIdent: String, token: String): MerOppfolgingStatusDTO {
        val senOppfolgingStatus = senOppfolgingService.prepareAndBuildSenOppfolgingStatusDTOV2(personIdent, token)

        // TODO: Replace
        val shouldDisplayKartlegging = false

        return when {
            senOppfolgingStatus.hasAccessToSenOppfolging ->
                MerOppfolgingStatusDTO.sen(senOppfolgingStatus)
            shouldDisplayKartlegging ->
                MerOppfolgingStatusDTO.kartlegging(hasSubmitted = true) // TODO
            else ->
                MerOppfolgingStatusDTO.ingen()
        }
    }
}
