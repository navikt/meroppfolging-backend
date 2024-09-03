package no.nav.syfo.varsel

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDate
import java.util.*

@Repository
class VarselDAO(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {

    val nyttVarselLimit = 106

    fun getUtsendtVarsel(fnr: String): UtsendtVarsel? {
        val sql = """
        select uuid, fnr, utsendt_tidspunkt, sykepengedager_id
        from UTSENDT_VARSEL
        where FNR = :FNR
        AND UTSENDT_TIDSPUNKT > NOW() - INTERVAL '$nyttVarselLimit' DAY
        """.trimIndent()

        val parameters = mapOf(
            "FNR" to fnr,
        )

        return namedParameterJdbcTemplate.query(sql, parameters, UtsendtVarselRowMapper()).firstOrNull()
    }

    fun storeUtsendtVarsel(fnr: String, sykepengeDagerId: String) {
        val sql = """
        INSERT INTO UTSENDT_VARSEL (UUID, FNR, UTSENDT_TIDSPUNKT, SYKEPENGEDAGER_ID)
        VALUES (:UUID, :FNR, :UTSENDT_TIDSPUNKT, :SYKEPENGEDAGER_ID)
        """.trimIndent()

        val parameters = mapOf(
            "UUID" to UUID.randomUUID(),
            "FNR" to fnr,
            "UTSENDT_TIDSPUNKT" to LocalDate.now(),
            "SYKEPENGEDAGER_ID" to sykepengeDagerId,
        )

        namedParameterJdbcTemplate.update(sql, parameters)
    }

    internal class UtsendtVarselRowMapper : RowMapper<UtsendtVarsel> {
        override fun mapRow(rs: ResultSet, rowNum: Int): UtsendtVarsel {
            return UtsendtVarsel(
                uuid = UUID.fromString(rs.getString("uuid")),
                fnr = rs.getString("fnr"),
                utsendtTidspunkt = rs.getTimestamp("utsendt_tidspunkt").toLocalDateTime(),
                sykepengedagerId = rs.getString("sykepengedager_id"),
            )
        }
    }
}
