package no.nav.syfo.varsel

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.io.Serializable
import java.time.LocalDateTime

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
sealed interface EsyfovarselHendelse : Serializable {
    val type: HendelseType
    val ferdigstill: Boolean?
    var data: Any?
}

data class ArbeidstakerHendelse(
    override val type: HendelseType,
    override val ferdigstill: Boolean?,
    override var data: Any?,
    val arbeidstakerFnr: String,
    val orgnummer: String?,
) : EsyfovarselHendelse

enum class HendelseType {
    SM_MER_VEILEDNING,
}

data class VarselData(
    val journalpost: VarselDataJournalpost? = null,
    val narmesteLeder: VarselDataNarmesteLeder? = null,
    val motetidspunkt: VarselDataMotetidspunkt? = null,
    val aktivitetskrav: VarselDataAktivitetskrav? = null,
)

data class VarselDataJournalpost(val uuid: String, val id: String?,)

data class VarselDataNarmesteLeder(val navn: String?,)

data class VarselDataMotetidspunkt(val tidspunkt: LocalDateTime,)

data class VarselDataAktivitetskrav(
    val sendForhandsvarsel: Boolean,
    val enableMicrofrontend: Boolean,
    val extendMicrofrontendDuration: Boolean,
)
