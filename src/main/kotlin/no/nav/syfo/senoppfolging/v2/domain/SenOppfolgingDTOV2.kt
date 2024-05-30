package no.nav.syfo.senoppfolging.v2.domain

import no.nav.syfo.besvarelse.database.domain.QuestionResponse

enum class SenOppfolgingQuestionTypeV2 {
    FREMTIDIG_SITUASJON,
    BEHOV_FOR_OPPFOLGING
}

enum class FremtidigSituasjonSvar {
    TILBAKE_HOS_ARBEIDSGIVER,
    TILBAKE_MED_TILPASNINGER,
    TILBAKE_GRADERT,
    BYTTE_JOBB,
    FORTSATT_SYK,
    USIKKER,
}

enum class BehovForOppfolgingSvar {
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

fun SenOppfolgingQuestionV2.toQuestionResponse(): QuestionResponse {
    return QuestionResponse(questionType.name, questionText, answerType, answerText)
}
