package no.nav.syfo.varsel

import org.springframework.stereotype.Service

@Service
class VarselService(
    private val producer: EsyfovarselProducer,
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
}
