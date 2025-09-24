package no.nav.syfo.kartlegging.database

import no.nav.syfo.kartlegging.domain.Kandidat
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp

@Repository
class KandidatDAO(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {

    fun persistKandidat(kandidat: Kandidat) {
        val insertStatement = """
        INSERT INTO KANDIDAT (
            uuid,
            fnr,
            status,
            created_at
        ) VALUES (:uuid, :fnr, :status, :created_at);
    """.trimIndent()

        val parameters = mapOf(
            "uuid" to kandidat.personIdent,
            "fnr" to kandidat.kandidatId,
            "status" to kandidat.status.name,
            "created_at" to Timestamp.from(kandidat.createdAt),
        )
        namedParameterJdbcTemplate.update(insertStatement, parameters)
    }

    fun findKandidatByFnr(fnr: String): Kandidat? {
        val selectStatement = """
            SELECT
                uuid,
                fnr,
                status,
                created_at
            FROM KANDIDAT
            WHERE fnr = :fnr
            ORDER BY created_at DESC
            LIMIT 1;
        """.trimIndent()

        val parameters = mapOf("fnr" to fnr)

        return namedParameterJdbcTemplate.query(selectStatement, parameters) { rs, _ -> rs.toKandidat() }
            .firstOrNull()
    }

    fun ResultSet.toKandidat(): Kandidat {
        return Kandidat(
            personIdent = this.getString("uuid"),
            kandidatId = this.getString("fnr"),
            status = enumValueOf(this.getString("status")),
            createdAt = this.getTimestamp("created_at").toInstant(),
        )
    }
}
