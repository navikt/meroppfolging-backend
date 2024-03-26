package no.nav.syfo.veilarbregistrering

data class StartRegistrationDTO(
    val registreringType: StartRegistrationType,
    val formidlingsgruppe: String,
    val servicegruppe: String,
    val rettighetsgruppe: String,
)

enum class StartRegistrationType { SYKMELDT_REGISTRERING, ORDINAER_REGISTRERING, ALLEREDE_REGISTRERT, REAKTIVERING }
