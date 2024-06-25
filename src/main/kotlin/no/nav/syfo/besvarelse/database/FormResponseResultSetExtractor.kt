package no.nav.syfo.besvarelse.database

import no.nav.syfo.besvarelse.database.domain.FormResponse
import no.nav.syfo.besvarelse.database.domain.FormType
import no.nav.syfo.domain.PersonIdentNumber
import org.springframework.jdbc.core.ResultSetExtractor
import java.sql.ResultSet
import java.util.UUID

class FormResponseResultSetExtractor : ResultSetExtractor<List<FormResponse>> {

    override fun extractData(rs: ResultSet): List<FormResponse>? {
        val formResponses = mutableListOf<FormResponse>()
        var currentFormResponse: FormResponse? = null

        while (rs.next()) {
            val responseId = rs.getObject("uuid", UUID::class.java)
            if (currentFormResponse == null || !currentFormResponse.uuid.equals(responseId)) {
                currentFormResponse = FormResponse(
                    uuid = responseId,
                    personIdent = PersonIdentNumber(rs.getString("person_ident")),
                    createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
                    formType = FormType.valueOf(rs.getString("form_type")),
                )
                formResponses.add(currentFormResponse)
            }

            val questionResponse = QuestionResponseRowMapper().mapRow(rs, rs.row)

            currentFormResponse.questionResponses.add(questionResponse)
        }

        return formResponses
    }
}
