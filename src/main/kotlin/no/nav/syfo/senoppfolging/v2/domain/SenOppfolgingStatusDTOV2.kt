package no.nav.syfo.senoppfolging.v2.domain

import no.nav.syfo.besvarelse.database.domain.QuestionResponse
import no.nav.syfo.senoppfolging.v1.domain.OnskerOppfolgingSvar

data class SenOppfolgingStatusDTOV2(
    val isPilot: Boolean,
    val responseStatus: ResponseStatus,
)

enum class ResponseStatus {
    NO_RESPONSE,
    TRENGER_OPPFOLGING,
    TRENGER_IKKE_OPPFOLGING,
}

fun List<QuestionResponse>.toResponseStatus(): ResponseStatus {
    val trengerOppfolgingQuestion = find { it.questionType == SenOppfolgingQuestionTypeV2.ONSKER_OPPFOLGING.name }
    if (trengerOppfolgingQuestion == null) {
        return ResponseStatus.NO_RESPONSE
    }

    val answer = OnskerOppfolgingSvar.valueOf(trengerOppfolgingQuestion.answerType)
    return when (answer) {
        OnskerOppfolgingSvar.JA -> ResponseStatus.TRENGER_OPPFOLGING
        OnskerOppfolgingSvar.NEI -> ResponseStatus.TRENGER_IKKE_OPPFOLGING
    }
}
