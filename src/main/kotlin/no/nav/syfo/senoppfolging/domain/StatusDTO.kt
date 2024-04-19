package no.nav.syfo.senoppfolging.domain

import no.nav.syfo.veilarbregistrering.domain.StartRegistrationType

data class StatusDTO(
    val registrationType: StartRegistrationType,
    val isSykmeldt: Boolean,
    val responseStatus: ResponseStatus
)

enum class ResponseStatus {
    NO_RESPONSE,
    TRENGER_OPPFOLGING,
    TRENGER_IKKE_OPPFOLGING,
}
