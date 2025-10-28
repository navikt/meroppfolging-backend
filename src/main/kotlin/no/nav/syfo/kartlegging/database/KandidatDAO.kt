package no.nav.syfo.kartlegging.database

import no.nav.syfo.kartlegging.domain.KartleggingssporsmalKandidat
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

@Repository
class KandidatDAO(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {

    fun persistKandidat(kandidat: KartleggingssporsmalKandidat) {
        val insertStatement = """
        INSERT INTO KARTLEGGINGSPORSMAL_KANDIDAT (
            kandidat_id,
            fnr,
            status,
            created_at
        ) VALUES (:kandidat_id, :fnr, :status, :created_at);
    """.trimIndent()

        val parameters = mapOf(
            "fnr" to kandidat.personIdent,
            "kandidat_id" to kandidat.kandidatId,
            "status" to kandidat.status.name,
            "created_at" to Timestamp.from(kandidat.createdAt),
        )
        namedParameterJdbcTemplate.update(insertStatement, parameters)
    }

    fun findKandidatByFnr(fnr: String): KartleggingssporsmalKandidat? {
        val selectStatement = """
            SELECT
                kandidat_id,
                fnr,
                status,
                created_at
            FROM KARTLEGGINGSPORSMAL_KANDIDAT
            WHERE fnr = :fnr
            ORDER BY created_at DESC
            LIMIT 1;
        """.trimIndent()

        val parameters = mapOf("fnr" to fnr)

        return namedParameterJdbcTemplate.query(selectStatement, parameters) { rs, _ -> rs.toKandidat() }
            .firstOrNull()
    }

    fun findKandidatByKandidatId(kandidatId: UUID): KartleggingssporsmalKandidat? {
        val selectStatement = """
            SELECT
                kandidat_id,
                fnr,
                status,
                created_at
            FROM KARTLEGGINGSPORSMAL_KANDIDAT
            WHERE kandidat_id = :kandidat_id
            ORDER BY created_at DESC
            LIMIT 1;
        """.trimIndent()

        val parameters = mapOf("kandidat_id" to kandidatId)

        return namedParameterJdbcTemplate.query(selectStatement, parameters) { rs, _ -> rs.toKandidat() }
            .firstOrNull()
    }

    fun ResultSet.toKandidat(): KartleggingssporsmalKandidat {
        return KartleggingssporsmalKandidat(
            personIdent = this.getString("fnr"),
            kandidatId = UUID.fromString(this.getString("kandidat_id")),
            status = enumValueOf(this.getString("status")),
            createdAt = this.getTimestamp("created_at").toInstant(),
        )
    }
}
