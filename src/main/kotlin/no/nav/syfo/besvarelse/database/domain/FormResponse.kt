package no.nav.syfo.besvarelse.database.domain

import no.nav.syfo.domain.PersonIdentNumber
import java.time.LocalDateTime
import java.util.UUID

data class FormResponse(
    val uuid: UUID,
    val personIdent: PersonIdentNumber,
    val createdAt: LocalDateTime,
    val questionResponses: List<QuestionResponse>,
    val type: FormType,
)

enum class FormType {
    SEN_OPPFOLGING_V1,
}
