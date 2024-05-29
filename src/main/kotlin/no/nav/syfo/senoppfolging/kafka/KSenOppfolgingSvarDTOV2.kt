package no.nav.syfo.senoppfolging.kafka

import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionV2
import java.time.LocalDateTime
import java.util.*

data class KSenOppfolgingSvarDTOV2(
    val id: UUID,
    val personIdent: String,
    val createdAt: LocalDateTime,
    val response: List<SenOppfolgingQuestionV2>,
)
