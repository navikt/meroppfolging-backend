package no.nav.syfo.besvarelse.database.domain

import no.nav.syfo.domain.PersonIdentNumber
import java.time.LocalDateTime
import java.util.UUID

data class FormResponse(
    val uuid: UUID,
    val personIdent: PersonIdentNumber,
    val createdAt: LocalDateTime,
    val formType: FormType,
    val questionResponses: MutableList<QuestionResponse> = mutableListOf(),
)
