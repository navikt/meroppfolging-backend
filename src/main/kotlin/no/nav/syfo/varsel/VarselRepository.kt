package no.nav.syfo.varsel

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.*

@Repository
class VarselRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    val gjenstaendeSykedagerLimit = 91
    val nyttVarselLimit = 106
    val maxDateLimit = 14

    fun getUtsendtVarsel(personIdent: String): UtsendtVarsel? {
        val sql =
            """
            select uuid, person_ident, utsendt_tidspunkt, utbetaling_id, sykmelding_id
            from UTSENDT_VARSEL
            where person_ident = :person_ident
            AND UTSENDT_TIDSPUNKT > NOW() - INTERVAL '$nyttVarselLimit' DAY
            """.trimIndent()

        val parameters =
            mapOf(
                "person_ident" to personIdent,
            )

        return namedParameterJdbcTemplate.query(sql, parameters, UtsendtVarselRowMapper()).firstOrNull()
    }

    fun storeUtsendtVarsel(
        personIdent: String,
        utbetalingId: String,
        sykmeldingId: String,
    ): UUID {
        val sql =
            """
            INSERT INTO UTSENDT_VARSEL (uuid, person_ident, utsendt_tidspunkt, utbetaling_id, sykmelding_id)
            VALUES (:uuid, :person_ident, :utsendt_tidspunkt, :utbetaling_id, :sykmelding_id)
            """.trimIndent()

        val utsendtVarselUUID = UUID.randomUUID()
        val parameters =
            mapOf(
                "uuid" to utsendtVarselUUID,
                "person_ident" to personIdent,
                "utsendt_tidspunkt" to LocalDateTime.now(),
                "utbetaling_id" to utbetalingId,
                "sykmelding_id" to sykmeldingId,
            )

        namedParameterJdbcTemplate.update(sql, parameters)
        return utsendtVarselUUID
    }

    fun fetchMerOppfolgingVarselToBeSent(): List<MerOppfolgingVarselDTO> {
        val sql = """
        WITH LatestEntry AS (
            SELECT 
                spdi.person_ident, 
                spdi.utbetaling_id, 
                sykmelding.sykmelding_id,
                utsendt_varsel.UTSENDT_TIDSPUNKT AS utsendt_tidspunkt_varsel,
                COPY_UTSENDT_VARSEL_ESYFOVARSEL.UTSENDT_TIDSPUNKT AS utsendt_tidspunkt_copy,
                ROW_NUMBER() OVER (PARTITION BY spdi.person_ident ORDER BY spdi.utbetaling_id DESC) AS row_num
            FROM sykepengedager_informasjon spdi
            LEFT JOIN UTSENDT_VARSEL utsendt_varsel ON spdi.utbetaling_id = utsendt_varsel.utbetaling_id
            LEFT JOIN COPY_UTSENDT_VARSEL_ESYFOVARSEL ON spdi.person_ident = COPY_UTSENDT_VARSEL_ESYFOVARSEL.fnr
            JOIN SYKMELDING sykmelding ON spdi.person_ident = sykmelding.employee_identification_number
            WHERE spdi.gjenstaende_sykedager < $gjenstaendeSykedagerLimit
              AND spdi.FORELOPIG_BEREGNET_SLUTT >= current_date + INTERVAL '$maxDateLimit' DAY
              AND sykmelding.tom > CURRENT_TIMESTAMP
              -- Fetch only if no varsel was sent recently
              AND (utsendt_varsel.UTSENDT_TIDSPUNKT IS NULL OR utsendt_varsel.UTSENDT_TIDSPUNKT <= NOW() - INTERVAL '$nyttVarselLimit' DAY)
              AND (COPY_UTSENDT_VARSEL_ESYFOVARSEL.UTSENDT_TIDSPUNKT IS NULL OR COPY_UTSENDT_VARSEL_ESYFOVARSEL.UTSENDT_TIDSPUNKT <= NOW() - INTERVAL '$nyttVarselLimit' DAY)
        ),
        SentVarsel AS (
            -- This CTE gathers all person_idents who already had a varsel sent
            SELECT DISTINCT spdi.person_ident
            FROM sykepengedager_informasjon spdi
            LEFT JOIN UTSENDT_VARSEL utsendt_varsel ON spdi.person_ident = utsendt_varsel.person_ident
            LEFT JOIN COPY_UTSENDT_VARSEL_ESYFOVARSEL ON spdi.person_ident = COPY_UTSENDT_VARSEL_ESYFOVARSEL.fnr
            WHERE utsendt_varsel.UTSENDT_TIDSPUNKT IS NOT NULL
               OR COPY_UTSENDT_VARSEL_ESYFOVARSEL.UTSENDT_TIDSPUNKT IS NOT NULL
        )
        -- Fetch the latest row per person_ident, excluding those with already sent varsel
        SELECT person_ident, utbetaling_id, sykmelding_id
        FROM LatestEntry
        WHERE row_num = 1
          AND person_ident NOT IN (SELECT person_ident FROM SentVarsel);
        """
        return namedParameterJdbcTemplate.query(sql, MerOppfolgingVarselDTOMapper())
    }
}

internal class UtsendtVarselRowMapper : RowMapper<UtsendtVarsel> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): UtsendtVarsel =
        UtsendtVarsel(
            uuid = UUID.fromString(rs.getString("uuid")),
            personIdent = rs.getString("person_ident"),
            utsendtTidspunkt = rs.getTimestamp("utsendt_tidspunkt").toLocalDateTime(),
            utbetalingId = rs.getString("utbetaling_id"),
            sykmeldingId = rs.getString("sykmelding_id"),
        )
}

internal class MerOppfolgingVarselDTOMapper : RowMapper<MerOppfolgingVarselDTO> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): MerOppfolgingVarselDTO =
        MerOppfolgingVarselDTO(
            personIdent = rs.getString("person_ident"),
            utbetalingId = rs.getString("utbetaling_id"),
            sykmeldingId = rs.getString("sykmelding_id"),
        )
}
