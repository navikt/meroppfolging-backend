package no.nav.syfo.varsel

import no.nav.syfo.pdl.PdlClient
import org.springframework.stereotype.Service

@Service
class VarselService(
    private val producer: EsyfovarselProducer,
    private val varselRepository: VarselRepository,
    private val pdlClient: PdlClient,
) {

    fun findMerOppfolgingVarselToBeSent(): List<MerOppfolgingVarselDTO> {
        return varselRepository.fetchMerOppfolgingVarselToBeSent()
            .filter {
                pdlClient.isBrukerYngreEnnGittMaxAlder(it.personIdent, 67)
            }
    }

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

    @Suppress("MaxLineLength")
    fun sendMerOppfolgingVarsel(
        merOppfolgingVarselDTO: MerOppfolgingVarselDTO,
    ) {
        // Hent PDF og journalfør. Avbryte utsending dersom journalføring feiler. Så sikrer vi at jobben prøver på nytt.
        val hendelse = ArbeidstakerHendelse(
            type = HendelseType.SM_MER_VEILEDNING,
            ferdigstill = false,
            data = null, // Må vel legge med journalpost_id her, og tilpasse Esyfovarsel slik at det blir distribuert til ikke-digitale brukere
            arbeidstakerFnr = merOppfolgingVarselDTO.personIdent,
            orgnummer = null,
        )
        producer.sendVarselTilEsyfovarsel(hendelse)
        // Legg til sending til isyfo
        varselRepository.storeUtsendtVarsel(
            personIdent = merOppfolgingVarselDTO.personIdent,
            utbetalingId = merOppfolgingVarselDTO.utbetalingId,
            sykmeldingId = merOppfolgingVarselDTO.sykmeldingId,
        )
    }

    fun getUtsendtVarsel(fnr: String): UtsendtVarsel? {
        return varselRepository.getUtsendtVarsel(fnr)
    }
}
