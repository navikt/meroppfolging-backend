package no.nav.syfo.besvarelse.database

import no.nav.syfo.besvarelse.database.domain.QuestionResponse
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet

class QuestionResponseRowMapper : RowMapper<QuestionResponse> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): QuestionResponse {
        return QuestionResponse(
            questionType = rs.getString("question_type"),
            questionText = rs.getString("question_text"),
            answerType = rs.getString("question_type"),
            answerText = rs.getString("question_text"),
        )
    }
}
