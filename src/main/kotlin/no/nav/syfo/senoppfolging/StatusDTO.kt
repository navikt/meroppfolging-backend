package no.nav.syfo.senoppfolging

import no.nav.syfo.veilarbregistrering.StartRegistrationType

data class StatusDTO(
    val registreringType: StartRegistrationType,
    val isSykmeldt: Boolean,
)
