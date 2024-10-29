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

    fun storeSkipVarselDueToAge(
        personIdent: String,
        fodselsdato: String?,
    ): UUID {
        val sql =
            """
            INSERT INTO SKIP_VARSELUTSENDING (person_ident, fodselsdato, created_at)
            VALUES (:person_ident, :fodselsdato, :created_at)
            """.trimIndent()

        val utsendtVarselUUID = UUID.randomUUID()
        val parameters =
            mapOf(
                "person_ident" to personIdent,
                "fodselsdato" to fodselsdato,
                "created_at" to LocalDateTime.now(),
            )

        namedParameterJdbcTemplate.update(sql, parameters)
        return utsendtVarselUUID
    }

    fun fetchMerOppfolgingVarselToBeSent(): List<MerOppfolgingVarselDTO> {
        val sql =
            """
            SELECT spdi.utbetaling_id, spdi.person_ident, spdi.gjenstaende_sykedager, spdi.forelopig_beregnet_slutt, sykmelding.sykmelding_id
            FROM sykepengedager_informasjon AS spdi
            JOIN SYKMELDING sykmelding ON spdi.person_ident = sykmelding.employee_identification_number
            WHERE spdi.utbetaling_id =
                (SELECT spdi2.utbetaling_id
                FROM sykepengedager_informasjon AS spdi2
                WHERE spdi.person_ident = spdi2.person_ident
                ORDER BY utbetaling_created_at DESC
                LIMIT 1)
            AND sykmelding.sykmelding_id = 
                (SELECT latest_sykmelding.sykmelding_id 
                FROM sykmelding AS latest_sykmelding
                WHERE sykmelding.employee_identification_number = latest_sykmelding.employee_identification_number
                ORDER BY latest_sykmelding.created_at DESC
                LIMIT 1)       
            AND spdi.gjenstaende_sykedager < $gjenstaendeSykedagerLimit
            AND sykmelding.tom > CURRENT_TIMESTAMP
            AND spdi.forelopig_beregnet_slutt >= current_date + INTERVAL '$maxDateLimit' DAY
            AND spdi.person_ident NOT IN
                (SELECT utsendt_varsel.person_ident
                FROM UTSENDT_VARSEL
                WHERE UTSENDT_TIDSPUNKT > NOW() - INTERVAL '$nyttVarselLimit' DAY)
            AND spdi.person_ident NOT IN
                (SELECT copy_utsendt_varsel_esyfovarsel.fnr
                FROM copy_utsendt_varsel_esyfovarsel
                WHERE UTSENDT_TIDSPUNKT > NOW() - INTERVAL '$nyttVarselLimit' DAY)
            AND spdi.person_ident NOT IN
                (SELECT skip_varselutsending.person_ident
                FROM skip_varselutsending);
            """.trimIndent()

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
