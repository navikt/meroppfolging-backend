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

data class SenOppfolgingDTOV2(val senOppfolgingFormV2: List<SenOppfolgingQuestionV2>,)

fun SenOppfolgingQuestionV2.toQuestionResponse(): QuestionResponse =
    QuestionResponse(questionType.name, questionText, answerType, answerText)

fun List<SenOppfolgingQuestionV2>.behovForOppfolging(): Boolean {
    val behovForOppfolgingSvar = this.filter { it.questionType == SenOppfolgingQuestionTypeV2.BEHOV_FOR_OPPFOLGING }

    require(behovForOppfolgingSvar.isNotEmpty()) { "Behov for oppfølging er ikke besvart" }

    return behovForOppfolgingSvar.firstOrNull { it.answerType == BehovForOppfolgingSvar.JA.name } != null
}

fun List<SenOppfolgingQuestionV2>.fremtidigSituasjonSvar(): FremtidigSituasjonSvar {
    val fremtidigSituasjonAnswer =
        this.firstOrNull { it.questionType == SenOppfolgingQuestionTypeV2.FREMTIDIG_SITUASJON }
            ?: throw IllegalArgumentException("Fremtidig situasjon er ikke besvart")

    return FremtidigSituasjonSvar.entries.first { it.name == fremtidigSituasjonAnswer.answerType }
}
