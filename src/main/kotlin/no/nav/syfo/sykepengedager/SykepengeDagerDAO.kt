package no.nav.syfo.sykepengedager

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Repository
class SykepengeDagerDAO(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun storeSykepengeDager(
        fnr: String,
        maksDato: LocalDate,
        utbetaltTom: LocalDate,
        gjenstaendeSykedager: Int,
        sykepengedagerId: String,
    ) {
        val sql = """
        INSERT INTO SYKEPENGEDAGER (UUID, FNR, MAKS_DATO, UTBETALT_TOM, GJENSTAENDE_SYKEDAGER, SYKEPENGEDAGER_ID, OPPRETTET)
        VALUES (:UUID, :FNR, :MAKS_DATO, :UTBETALT_TOM, :GJENSTAENDE_SYKEDAGER, :SYKEPENGEDAGER_ID, :OPPRETTET)
        """.trimIndent()

        val parameters = mapOf(
            "UUID" to UUID.randomUUID(),
            "FNR" to fnr,
            "MAKS_DATO" to maksDato,
            "UTBETALT_TOM" to utbetaltTom,
            "GJENSTAENDE_SYKEDAGER" to gjenstaendeSykedager,
            "SYKEPENGEDAGER_ID" to sykepengedagerId,
            "OPPRETTET" to LocalDateTime.now(),
        )

        namedParameterJdbcTemplate.update(sql, parameters)
    }
}

