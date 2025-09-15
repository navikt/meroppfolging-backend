package no.nav.syfo.mikrofrontend.domain

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingStatusDTOV2

sealed class MerOppfolgingStatusDTO {
    abstract val oppfolgingsType: OppfolgingsType
    abstract val senOppfolgingStatus: SenOppfolgingStatusDTOV2?

    data class SenOppfolging(
        @field:JsonInclude(JsonInclude.Include.ALWAYS)
        override val senOppfolgingStatus: SenOppfolgingStatusDTOV2,
    ) : MerOppfolgingStatusDTO() {
        override val oppfolgingsType: OppfolgingsType = OppfolgingsType.SEN_OPPFOLGING
    }

    data class Kartlegging(
        val hasSubmittedKartlegging: Boolean,
        @field:JsonInclude(JsonInclude.Include.ALWAYS)
        override val senOppfolgingStatus: SenOppfolgingStatusDTOV2? = null,
    ) : MerOppfolgingStatusDTO() {
        override val oppfolgingsType: OppfolgingsType = OppfolgingsType.KARTLEGGING
    }

    data class IngenOppfolging(
        @field:JsonInclude(JsonInclude.Include.ALWAYS)
        override val senOppfolgingStatus: SenOppfolgingStatusDTOV2? = null,
    ) : MerOppfolgingStatusDTO() {
        override val oppfolgingsType: OppfolgingsType = OppfolgingsType.INGEN_OPPFOLGING
    }

    companion object {
        fun sen(status: SenOppfolgingStatusDTOV2) = SenOppfolging(status)
        fun kartlegging(hasSubmitted: Boolean) = Kartlegging(hasSubmitted)
        fun ingen() = IngenOppfolging()
    }
}
