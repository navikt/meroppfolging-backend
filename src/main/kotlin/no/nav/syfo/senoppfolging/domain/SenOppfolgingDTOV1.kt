package no.nav.syfo.senoppfolging.domain

data class SenOppfolgingDTOV1(
    val senOppfolgingRegistrering: SenOppfolgingRegistrering?,
    val senOppfolgingFormV1: List<SenOppfolgingQuestionV1>,
)
