package no.nav.syfo.senoppfolging.v1.domain

data class SenOppfolgingDTOV1(
    val senOppfolgingRegistrering: SenOppfolgingRegistrering?,
    val senOppfolgingFormV1: List<SenOppfolgingQuestionV1>,
)
