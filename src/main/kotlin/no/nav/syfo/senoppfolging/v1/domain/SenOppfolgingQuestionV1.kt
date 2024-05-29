package no.nav.syfo.senoppfolging.v1.domain

data class SenOppfolgingQuestionV1(
    val questionType: SenOppfolgingQuestionTypeV1,
    val questionText: String,
    val answerType: String,
    val answerText: String,
)

enum class OnskerOppfolgingSvar {
    JA,
    NEI
}
