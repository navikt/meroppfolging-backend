package no.nav.syfo.senoppfolging.v2.domain

import no.nav.syfo.besvarelse.database.domain.QuestionResponse
import no.nav.syfo.senoppfolging.v1.domain.OnskerOppfolgingSvar

enum class SenOppfolgingQuestionTypeV2 {
    FREMTIDIG_SITUASJON,
    ONSKER_OPPFOLGING
}

enum class FremtidigSituasjonSvar {
    TILBAKE_HOS_ARBEIDSGIVER,
    TILBAKE_MED_TILPASNINGER,
    FORTSATT_SYK,
    USIKKER
}

enum class OnskerOppfolgingSvar {
    JA,
    NEI
}

data class SenOppfolgingQuestionV2(
    val questionType: SenOppfolgingQuestionTypeV2,
    val questionText: String,
    val answerType: String,
    val answerText: String,
)

data class SenOppfolgingDTOV2(
    val senOppfolgingFormV2: List<SenOppfolgingQuestionV2>,
)

fun List<SenOppfolgingQuestionV2>.trengerOppfolging(): Boolean {
    val trengerOppfolgingQuestion = find { it.questionType == SenOppfolgingQuestionTypeV2.ONSKER_OPPFOLGING }
    checkNotNull(trengerOppfolgingQuestion)
    val answer = OnskerOppfolgingSvar.valueOf(trengerOppfolgingQuestion.answerType)
    return answer == OnskerOppfolgingSvar.JA
}

fun SenOppfolgingQuestionV2.toQuestionResponse(): QuestionResponse {
    return QuestionResponse(questionType.name, questionText, answerType, answerText)
}
