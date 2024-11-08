package no.nav.syfo.senoppfolging.v2.domain

import no.nav.syfo.besvarelse.database.domain.QuestionResponse

data class SenOppfolgingStatusDTOV2(
    val responseStatus: ResponseStatus,
    val response: List<QuestionResponse>?,
    val responseTime: String?,
    val maxDate: String?,
    val gjenstaendeSykedager: String?,
    val hasAccessToSenOppfolging: Boolean,
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
