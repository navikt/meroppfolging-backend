package no.nav.syfo.mikrofrontend.domain

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.syfo.kartlegging.domain.PersistedKartleggingssporsmal
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingStatusDTOV2
import java.time.format.DateTimeFormatter

sealed class MerOppfolgingStatusDTO {
    abstract val oppfolgingsType: OppfolgingsType

    data class SenOppfolging(
        @field:JsonInclude(JsonInclude.Include.ALWAYS)
        val senOppfolgingStatus: SenOppfolgingStatusDTOV2,
    ) : MerOppfolgingStatusDTO() {
        override val oppfolgingsType: OppfolgingsType = OppfolgingsType.SEN_OPPFOLGING
    }

    data class Kartlegging(
        @field:JsonInclude(JsonInclude.Include.ALWAYS)
        val kartleggingStatus: KartleggingStatusDTO,
    ) : MerOppfolgingStatusDTO() {
        override val oppfolgingsType: OppfolgingsType = OppfolgingsType.KARTLEGGING
    }

    data class IngenOppfolging(
        @field:JsonInclude(JsonInclude.Include.ALWAYS)
        val senOppfolgingStatus: SenOppfolgingStatusDTOV2? = null,
    ) : MerOppfolgingStatusDTO() {
        override val oppfolgingsType: OppfolgingsType = OppfolgingsType.INGEN_OPPFOLGING
    }
}

enum class KartleggingResponseStatusType {
    NO_RESPONSE,
    SUBMITTED,
}

data class KartleggingStatusDTO(
    val responseStatus: KartleggingResponseStatusType,
    val responseDateTime: String?,
    val hasAccessToKartlegging: Boolean,
)

fun PersistedKartleggingssporsmal?.toKartleggingStatusDTO(hasAccessToKartlegging: Boolean,): KartleggingStatusDTO =
    if (this == null) {
        KartleggingStatusDTO(
            responseStatus = KartleggingResponseStatusType.NO_RESPONSE,
            responseDateTime = null,
            hasAccessToKartlegging = hasAccessToKartlegging,
        )
    } else {
        KartleggingStatusDTO(
            responseStatus = KartleggingResponseStatusType.SUBMITTED,
            responseDateTime = DateTimeFormatter.ISO_INSTANT.format(createdAt),
            hasAccessToKartlegging = hasAccessToKartlegging,
        )
    }
