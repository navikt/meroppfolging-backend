package no.nav.syfo.kartlegging.database

import no.nav.syfo.kartlegging.domain.Kartleggingssporsmal
import no.nav.syfo.kartlegging.domain.PersistedKartleggingssporsmal
import no.nav.syfo.kartlegging.domain.formsnapshot.FormSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.jsonToFormSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.toJsonString
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

@Repository
class KartleggingssporsmalDAO(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {

    fun persistKartleggingssporsmal(
        kartleggingssporsmal: Kartleggingssporsmal,
    ): UUID {
        val insertStatement = """
        INSERT INTO KARTLEGGINGSPORSMAL (
            fnr,
            form_snapshot,
            created_at
        ) VALUES (:fnr, :form_snapshot::jsonb, NOW())
        RETURNING uuid;
    """.trimIndent()

        val parameters = mapOf(
            "fnr" to kartleggingssporsmal.fnr,
            "form_snapshot" to kartleggingssporsmal.formSnapshot.toJsonString(),
        )
        return namedParameterJdbcTemplate.queryForObject(insertStatement, parameters, UUID::class.java)
            ?: throw IllegalStateException("Failed to persist kartleggingssporsmal (no uuid returned)")
    }

    fun getLatestKartleggingssporsmalByFnr(fnr: String): PersistedKartleggingssporsmal? {
        val selectStatement = """
            SELECT
                uuid,
                fnr,
                form_snapshot,
                created_at
            FROM KARTLEGGINGSPORSMAL
            WHERE fnr = :fnr
            ORDER BY created_at DESC;
        """.trimIndent()

        val parameters = mapOf("fnr" to fnr)

        return namedParameterJdbcTemplate.query(selectStatement, parameters) { rs, _ -> rs.toKartleggingssporsmal() }
            .firstOrNull()
    }

    fun getKartleggingssporsmalByUuid(uuid: UUID): PersistedKartleggingssporsmal? {
        val selectStatement = """
            SELECT
                uuid,
                fnr,
                form_snapshot,
                created_at
            FROM KARTLEGGINGSPORSMAL
            WHERE uuid = :uuid;
        """.trimIndent()

        val parameters = mapOf("uuid" to uuid)

        return namedParameterJdbcTemplate.query(selectStatement, parameters) { rs, _ -> rs.toKartleggingssporsmal() }
            .firstOrNull()
    }

    fun ResultSet.toKartleggingssporsmal(): PersistedKartleggingssporsmal = PersistedKartleggingssporsmal(
        uuid = getObject("uuid", UUID::class.java),
        fnr = getString("fnr"),
        formSnapshot = getString("form_snapshot").let { FormSnapshot.jsonToFormSnapshot(it) },
        createdAt = getTimestamp("created_at").toInstant()
    )
}
