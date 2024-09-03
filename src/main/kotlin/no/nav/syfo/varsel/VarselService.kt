package no.nav.syfo.varsel

import org.springframework.stereotype.Service

@Service
class VarselService(
    private val producer: EsyfovarselProducer,
    private val varselDAO: VarselDAO,
) {
    fun ferdigstillMerOppfolgingVarsel(
        fnr: String,
    ) {
        val hendelse = ArbeidstakerHendelse(
            type = HendelseType.SM_MER_VEILEDNING,
            ferdigstill = true,
            data = null,
            arbeidstakerFnr = fnr,
            orgnummer = null,
        )
        producer.sendVarselTilEsyfovarsel(hendelse)
    }

    fun sendMerOppfolgingVarsel(
        fnr: String,
    ) {
        val hendelse = ArbeidstakerHendelse(
            type = HendelseType.SM_MER_VEILEDNING,
            ferdigstill = false,
            data = null,
            arbeidstakerFnr = fnr,
            orgnummer = null,
        )
        producer.sendVarselTilEsyfovarsel(hendelse)
    }

    fun getUtsendtVarsel(fnr: String): UtsendtVarsel? {
        return varselDAO.getUtsendtVarsel(fnr)
    }

    fun storeUtsendtVarsel(fnr: String, sykepengeDagerId: String) {
        varselDAO.storeUtsendtVarsel(fnr, sykepengeDagerId)
    }
}
