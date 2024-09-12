package no.nav.syfo.sykepengedager

import org.springframework.stereotype.Service

@Service
class SykepengeDagerService(
    private val sykepengeDagerDAO: SykepengeDagerDAO,
) {


    // Flytt til jobb-kode
//    fun shouldSendVarsel(sykepengeDagerDTO: SykepengeDagerDTO): Boolean {
//        val hasLimitedRemainingSykedager: Boolean = sykepengeDagerDTO.gjenstaendeSykedager < gjenstaendeSykedagerLimit
//        val maxDateIsEqualToOrMoreThanTwoWeeksAway: Boolean =
//            sykepengeDagerDTO.maksDato.isAfter(LocalDate.now().plusDays(maxDateLimit))
//        val hasAlreadySentVarsel: Boolean = varselService.getUtsendtVarsel(sykepengeDagerDTO.fnr) != null
//        val isBrukerYngreEnn67Ar: Boolean = pdlClient.isBrukerYngreEnnGittMaxAlder(sykepengeDagerDTO.fnr, 67)
//        val hasActiveSykmelding = true // implement this
//
//        return hasLimitedRemainingSykedager &&
//            maxDateIsEqualToOrMoreThanTwoWeeksAway &&
//            !hasAlreadySentVarsel &&
//            isBrukerYngreEnn67Ar
//    }

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
