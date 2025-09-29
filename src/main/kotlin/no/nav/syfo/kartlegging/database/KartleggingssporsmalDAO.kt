package no.nav.syfo.kartlegging.database

import no.nav.syfo.kartlegging.domain.Kartleggingssporsmal
import no.nav.syfo.kartlegging.domain.PersistedKartleggingssporsmal
import no.nav.syfo.kartlegging.domain.formsnapshot.FormSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.jsonToFormSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.toJsonString
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Repository
class KartleggingssporsmalDAO(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {

    fun persistKartleggingssporsmal(
        kartleggingssporsmal: Kartleggingssporsmal,
        createdAt: Instant = Instant.now(),
    ): UUID {
        val insertStatement = """
        INSERT INTO KARTLEGGINGSPORSMAL (
            fnr,
            kandidat_id,
            form_snapshot,
            created_at
        ) VALUES (:fnr, :kandidat_id, :form_snapshot::jsonb, :created_at)
        RETURNING uuid;
    """.trimIndent()

        val parameters = mapOf(
            "fnr" to kartleggingssporsmal.fnr,
            "kandidat_id" to kartleggingssporsmal.kandidatId,
            "form_snapshot" to kartleggingssporsmal.formSnapshot.toJsonString(),
            "created_at" to Timestamp.from(createdAt),
        )
        return namedParameterJdbcTemplate.queryForObject(insertStatement, parameters, UUID::class.java)
            ?: throw IllegalStateException("Failed to persist kartleggingssporsmal (no uuid returned)")
    }

    fun getLatestKartleggingssporsmalByKandidatId(kandidatId: UUID): PersistedKartleggingssporsmal? {
        val selectStatement = """
            SELECT
                uuid,
                fnr,
                kandidat_id,
                form_snapshot,
                created_at
            FROM KARTLEGGINGSPORSMAL
            WHERE kandidat_id = :kandidat_id
            ORDER BY created_at DESC;
        """.trimIndent()

        val parameters = mapOf("kandidat_id" to kandidatId)

        return namedParameterJdbcTemplate.query(selectStatement, parameters) { rs, _ -> rs.toKartleggingssporsmal() }
            .firstOrNull()
    }

    fun getKartleggingssporsmalByUuid(uuid: UUID): PersistedKartleggingssporsmal? {
        val selectStatement = """
            SELECT
                uuid,
                fnr,
                kandidat_id,
                form_snapshot,
                created_at
            FROM KARTLEGGINGSPORSMAL
            WHERE uuid = :uuid;
        """.trimIndent()

        val parameters = mapOf("uuid" to uuid)

        return namedParameterJdbcTemplate.query(selectStatement, parameters) { rs, _ -> rs.toKartleggingssporsmal() }
            .firstOrNull()
    }

    fun setJournalpostIdForKartleggingssporsmal(uuid: UUID, journalpostId: String) {
        val updateStatement = """
            UPDATE KARTLEGGINGSPORSMAL
            SET journalpost_id = :journalpost_id
            WHERE uuid = :uuid;
        """.trimIndent()

        val parameters = mapOf(
            "uuid" to uuid,
            "journalpost_id" to journalpostId,
        )

        namedParameterJdbcTemplate.update(updateStatement, parameters)
    }

    fun getKartleggingssporsmalNotJournaled(): List<PersistedKartleggingssporsmal> {
        val selectStatement = """
            SELECT
                uuid,
                fnr,
                kandidat_id,
                form_snapshot,
                created_at
            FROM KARTLEGGINGSPORSMAL
            WHERE journalpost_id IS NULL;
        """.trimIndent()

        return namedParameterJdbcTemplate.query(selectStatement) { rs, _ -> rs.toKartleggingssporsmal() }
    }

    fun ResultSet.toKartleggingssporsmal(): PersistedKartleggingssporsmal = PersistedKartleggingssporsmal(
        uuid = getObject("uuid", UUID::class.java),
        fnr = getString("fnr"),
        kandidatId = getObject("kandidat_id", UUID::class.java),
        formSnapshot = getString("form_snapshot").let { FormSnapshot.jsonToFormSnapshot(it) },
        createdAt = getTimestamp("created_at").toInstant(),
    )
}
