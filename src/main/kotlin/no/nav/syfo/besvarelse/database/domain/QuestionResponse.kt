package no.nav.syfo.besvarelse.database.domain

data class QuestionResponse(
    val questionType: String,
    val questionText: String,
    val answerType: String,
    val answerText: String,
)

fun List<QuestionResponse>.toFixedOrder(): List<QuestionResponse> {
    val questionResponsesInFixedOrder = mutableListOf<QuestionResponse>()

    this.find { it.questionType == "FREMTIDIG_SITUASJON" }?.let {
        questionResponsesInFixedOrder.add(it)
    }

    this.find { it.questionType == "BEHOV_FOR_OPPFOLGING" }?.let {
        questionResponsesInFixedOrder.add(it)
    }

    this.filterNot {
        it.questionType == "FREMTIDIG_SITUASJON" || it.questionType == "BEHOV_FOR_OPPFOLGING"
    }.let {
        questionResponsesInFixedOrder.addAll(it)
    }

    return questionResponsesInFixedOrder
}
