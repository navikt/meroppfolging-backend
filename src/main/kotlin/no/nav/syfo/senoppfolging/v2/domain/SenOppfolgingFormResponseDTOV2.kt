package no.nav.syfo.senoppfolging.v2.domain

import no.nav.syfo.besvarelse.database.domain.QuestionResponse
import java.time.LocalDateTime

data class SenOppfolgingFormResponseDTOV2(
    val uuid: String,
    val personIdent: String,
    val createdAt: LocalDateTime,
    val formType: String,
    val questionResponses: List<QuestionResponseDTO>
)

data class QuestionResponseDTO(
    val questionType: String,
    val questionText: String,
    val answerType: String,
    val answerText: String,
)

fun List<QuestionResponse>.toQuestionResponseDTOs(): List<QuestionResponseDTO> = this.map {
    QuestionResponseDTO(
        questionType = it.questionType,
        questionText = it.questionText,
        answerType = it.answerType,
        answerText = it.answerText,
    )
}
