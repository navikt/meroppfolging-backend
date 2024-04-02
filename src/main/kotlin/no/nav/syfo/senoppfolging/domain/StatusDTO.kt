package no.nav.syfo.senoppfolging.domain

import no.nav.syfo.veilarbregistrering.domain.StartRegistrationType

data class StatusDTO(
    val registrationType: StartRegistrationType,
    val isSykmeldt: Boolean,
)
