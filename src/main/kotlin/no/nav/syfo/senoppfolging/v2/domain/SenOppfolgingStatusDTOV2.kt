package no.nav.syfo.senoppfolging.v2.domain

import no.nav.syfo.besvarelse.database.domain.QuestionResponse
import java.time.LocalDate
import java.time.LocalDateTime

data class SenOppfolgingStatusDTOV2(
    val responseStatus: ResponseStatus,
    val response: List<QuestionResponse>?,
    val responseDateTime: LocalDateTime?,
    val maxDate: LocalDate?,
    val utbetaltTomDate: LocalDate?,
    val gjenstaendeSykedager: Int?,
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

fun List<QuestionResponse>.sortForMeroppfolgingFrontend(): List<QuestionResponse> = this.sortedBy {
    when (it.questionType) {
        SenOppfolgingQuestionTypeV2.FREMTIDIG_SITUASJON.name -> 0
        SenOppfolgingQuestionTypeV2.BEHOV_FOR_OPPFOLGING.name -> 1
        else -> Int.MAX_VALUE
    }
}
