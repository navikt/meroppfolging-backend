package no.nav.syfo.sykepengedager

import no.nav.syfo.varsel.VarselService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class SykepengeDagerService(
    private val varselService: VarselService,
) {
    val gjenstaendeSykedagerLimit = 91
    val maxDateLimit = 13L

    fun processSykepengeDagerRecord(sykepengeDagerDTO: SykepengeDagerDTO) {
        val hasLimitedRemainingSykedager: Boolean = sykepengeDagerDTO.gjenstaendeSykedager < gjenstaendeSykedagerLimit
        val maxDateIsEqualToOrMoreThanTwoWeeksAway: Boolean =
            sykepengeDagerDTO.maksDato.isAfter(LocalDate.now().plusDays(maxDateLimit))
        val hasAlreadySentVarsel: Boolean = varselService.getUtsendtVarsel(sykepengeDagerDTO.fnr) != null

        // sjekk isBrukerYngreEnn67Ar
        // sjekk isPersonSykmeldtPaDato - hva gjør vi her, resend-jobb?

        if (hasLimitedRemainingSykedager && maxDateIsEqualToOrMoreThanTwoWeeksAway && !hasAlreadySentVarsel) {
            varselService.sendMerOppfolgingVarsel(sykepengeDagerDTO.fnr)
            varselService.storeUtsendtVarsel(
                fnr = sykepengeDagerDTO.fnr,
                sykepengeDagerId = sykepengeDagerDTO.id,
            )
            // Publiser til isyfo-kafka
        }
    }
}
