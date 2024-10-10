package no.nav.syfo.varsel.database

import no.nav.syfo.varsel.MerOppfolgingVarselDTO
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class SendingDueDateDAO(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun persistSendingDueDate(merOppfolgingVarselDTO: MerOppfolgingVarselDTO) {
        val insertStatement = """
            INSERT INTO SENDING_DUE_DATE (
                person_ident,
                utbetaling_id,
                sykmelding_id,
                last_day_for_sending
            ) VALUES (:person_ident, :utbetaling_id, :sykmelding_id, :last_day_for_sending)
            ON CONFLICT (person_ident, utbetaling_id, sykmelding_id, last_day_for_sending) DO NOTHING 
            ;
        """.trimIndent()

        val parameters = MapSqlParameterSource()
            .addValue("person_ident", merOppfolgingVarselDTO.personIdent)
            .addValue("utbetaling_id", merOppfolgingVarselDTO.utbetalingId)
            .addValue("sykmelding_id", merOppfolgingVarselDTO.sykmeldingId)
            .addValue("last_day_for_sending", Timestamp.valueOf(LocalDateTime.now().plusDays(2)))
        namedParameterJdbcTemplate.update(insertStatement, parameters)
    }

    fun getSendingDueDate(
        personIdent: String,
        sykmeldingId: String,
        utbetalingId: String,
    ): LocalDateTime? {
        val selectStatement = """
        SELECT *
        FROM SENDING_DUE_DATE
        WHERE person_ident = :person_ident AND sykmelding_id = :sykmelding_id
        ORDER BY created_at DESC
        """.trimIndent()

        val parameters = MapSqlParameterSource()
            .addValue("person_ident", personIdent)
            .addValue("utbetalingId", utbetalingId)
            .addValue("sykmeldingId", sykmeldingId)

        return namedParameterJdbcTemplate.query(selectStatement, parameters) { rs, _ -> rs.getDueDate() }.firstOrNull()
    }

    fun deleteSendingDueDate(sykmeldingId: String) {
        val deleteStatement = """
        DELETE FROM SENDING_DUE_DATE
        WHERE sykmelding_id = :sykmelding_id
        """.trimIndent()

        val parameters = MapSqlParameterSource()
            .addValue("sykmelding_id", sykmeldingId)

        namedParameterJdbcTemplate.update(deleteStatement, parameters)
    }

    fun ResultSet.getDueDate(): LocalDateTime = getTimestamp("last_day_for_sending").toLocalDateTime()
}
