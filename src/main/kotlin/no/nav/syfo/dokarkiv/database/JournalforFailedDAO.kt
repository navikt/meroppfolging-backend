package no.nav.syfo.dokarkiv.database

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class JournalforFailedDAO(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,) {
    fun persistJournalforFailed(fnr: String, pdf: ByteArray, varselUuid: String, failReason: String) {
        val uuid = java.util.UUID.randomUUID()
        val insertStatement = """
            INSERT INTO JOURNALFORING_FAILED (
                    UUID,   
                    varsel_uuid,
                    pdf,
                    person_ident,
                    received_at,
                    fail_reason
            ) VALUES (:UUID, :varsel_uuid, :pdf, :person_ident, :received_at,  :fail_reason)
            ;
        """.trimIndent()

        val parameters = MapSqlParameterSource()
            .addValue("UUID", uuid)
            .addValue("varsel_uuid", varselUuid)
            .addValue("pdf", pdf)
            .addValue("person_ident", fnr)
            .addValue("received_at", Timestamp.valueOf(LocalDateTime.now()))
            .addValue("fail_reason", failReason)

        namedParameterJdbcTemplate.update(insertStatement, parameters)
    }

    fun fetchJournalforFailed(): List<PJournalforFailed>? {
        val selectStatement = """
            SELECT *
            FROM JOURNALFORING_FAILED
        """.trimIndent()

        return namedParameterJdbcTemplate.query(
            selectStatement,
        ) { rs, _ -> rs.toJournalforFailed() }
    }

    fun ResultSet.toJournalforFailed() = PJournalforFailed(
        uuid = getString("UUID"),
        varselUuid = getString("varsel_uuid"),
        pdf = getBytes("pdf"),
        personIdent = getString("person_ident"),
        receivedAt = getTimestamp("received_at").toLocalDateTime(),
        failReason = getString("fail_reason"),
    )

    data class PJournalforFailed(
        val uuid: String,
        val varselUuid: String,
        val pdf: ByteArray,
        val personIdent: String,
        val receivedAt: LocalDateTime,
        val failReason: String,
    )
}
