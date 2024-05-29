package no.nav.syfo.besvarelse.database

import no.nav.syfo.besvarelse.database.domain.FormType
import no.nav.syfo.besvarelse.database.domain.QuestionResponse
import no.nav.syfo.domain.PersonIdentNumber
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Repository
class ResponseDao(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun saveFormResponse(
        personIdent: PersonIdentNumber,
        questionResponses: List<QuestionResponse>,
        formType: FormType,
        createdAt: LocalDateTime,
    ): UUID {
        val uuid = UUID.randomUUID()
        val lagreSql =
            """
            INSERT INTO FORM_RESPONSE (
                uuid,
                person_ident,
                created_at,
                form_type
            )
            VALUES (
                :uuid,
                :person_ident,
                :created_at,
                :form_type
            )
            """.trimIndent()
        val mapLagreSql =
            MapSqlParameterSource()
                .addValue("uuid", uuid)
                .addValue("person_ident", personIdent.value)
                .addValue("created_at", Timestamp.valueOf(createdAt))
                .addValue("form_type", formType.name)
        namedParameterJdbcTemplate.update(lagreSql, mapLagreSql)
        questionResponses.forEach {
            saveQuestionResponse(it, uuid, createdAt)
        }
        return uuid
    }

    fun saveQuestionResponse(
        questionResponse: QuestionResponse,
        besvarelseUuid: UUID,
        createdAt: LocalDateTime,
    ): UUID {
        val uuid = UUID.randomUUID()
        val lagreSql =
            """
            INSERT INTO QUESTION_RESPONSE (
                uuid,
                created_at,
                question_type,
                question_text,
                answer_type,
                answer_text,
                response_id
            )
            VALUES (
                :uuid,
                :created_at,
                :question_type,
                :question_text,
                :answer_type,
                :answer_text,
                :response_id
            )
            """.trimIndent()
        val mapLagreSql =
            MapSqlParameterSource()
                .addValue("uuid", uuid)
                .addValue("created_at", createdAt)
                .addValue("question_type", questionResponse.questionType)
                .addValue("question_text", questionResponse.questionText)
                .addValue("answer_type", questionResponse.answerType)
                .addValue("answer_text", questionResponse.answerText)
                .addValue("response_id", besvarelseUuid)
        namedParameterJdbcTemplate.update(lagreSql, mapLagreSql)
        return uuid
    }

    @Suppress("SwallowedException")
    fun find(personIdent: PersonIdentNumber, formType: FormType, from: LocalDate): List<QuestionResponse> {
        val query = """
            SELECT 
                question.question_type,
                question.question_text,
                question.answer_type,
                question.answer_text
            FROM QUESTION_RESPONSE question
            JOIN FORM_RESPONSE form ON question.response_id = form.uuid
            WHERE form.person_ident = :person_ident
            AND form.form_type = :form_type
            AND form.created_at > :from_date
        """.trimIndent()

        val namedParameters = MapSqlParameterSource()
            .addValue("person_ident", personIdent.value)
            .addValue("form_type", formType.name)
            .addValue("from_date", Date.valueOf(from))

        return try {
            namedParameterJdbcTemplate.query(query, namedParameters, QuestionResponseRowMapper())
        } catch (e: EmptyResultDataAccessException) {
            emptyList()
        }
    }
}
