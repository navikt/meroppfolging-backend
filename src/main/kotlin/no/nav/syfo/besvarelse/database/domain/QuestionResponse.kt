package no.nav.syfo.besvarelse.database.domain

data class QuestionResponse(
    val questionType: String,
    val questionText: String,
    val answerType: String,
    val answerText: String,
)
