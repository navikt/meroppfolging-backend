package no.nav.syfo.senoppfolging.v2.domain

import no.nav.syfo.besvarelse.database.domain.FormResponse
import no.nav.syfo.besvarelse.database.domain.QuestionResponse

data class SenOppfolgingStatusDTOV2(
    val isPilot: Boolean,
    val responseStatus: ResponseStatus,
    val response: FormResponse?,
    val responseTime: String?,
    val maxDate: String?,
)

enum class ResponseStatus {
    NO_RESPONSE,
    TRENGER_OPPFOLGING,
    TRENGER_IKKE_OPPFOLGING,
}

fun List<QuestionResponse>.toResponseStatus(): ResponseStatus {
    val trengerOppfolgingQuestion = find { it.questionType == SenOppfolgingQuestionTypeV2.BEHOV_FOR_OPPFOLGING.name }
    if (trengerOppfolgingQuestion == null) {
        return ResponseStatus.NO_RESPONSE
    }

    val answer = BehovForOppfolgingSvar.valueOf(trengerOppfolgingQuestion.answerType)
    return when (answer) {
        BehovForOppfolgingSvar.JA -> ResponseStatus.TRENGER_OPPFOLGING
        BehovForOppfolgingSvar.NEI -> ResponseStatus.TRENGER_IKKE_OPPFOLGING
    }
}
