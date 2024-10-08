package no.nav.syfo.varsel

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDate
import java.util.*

@Repository
class VarselRepository(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    val gjenstaendeSykedagerLimit = 91
    val nyttVarselLimit = 106
    val maxDateLimit = 14

    fun getUtsendtVarsel(personIdent: String): UtsendtVarsel? {
        val sql = """
        select uuid, person_ident, utsendt_tidspunkt, utbetaling_id, sykmelding_id
        from UTSENDT_VARSEL
        where person_ident = :person_ident
        AND UTSENDT_TIDSPUNKT > NOW() - INTERVAL '$nyttVarselLimit' DAY
        """.trimIndent()

        val parameters = mapOf(
            "person_ident" to personIdent,
        )

        return namedParameterJdbcTemplate.query(sql, parameters, UtsendtVarselRowMapper()).firstOrNull()
    }

    fun storeUtsendtVarsel(personIdent: String, utbetalingId: String, sykmeldingId: String): UUID {
        val sql = """
        INSERT INTO UTSENDT_VARSEL (uuid, person_ident, utsendt_tidspunkt, utbetaling_id, sykmelding_id)
        VALUES (:uuid, :person_ident, :utsendt_tidspunkt, :utbetaling_id, :sykmelding_id)
        """.trimIndent()

        val utsendtVarselUUID = UUID.randomUUID()
        val parameters = mapOf(
            "uuid" to utsendtVarselUUID,
            "person_ident" to personIdent,
            "utsendt_tidspunkt" to LocalDate.now(),
            "utbetaling_id" to utbetalingId,
            "sykmelding_id" to sykmeldingId,
        )

        namedParameterJdbcTemplate.update(sql, parameters)
        return utsendtVarselUUID
    }

    fun fetchMerOppfolgingVarselToBeSent(): List<MerOppfolgingVarselDTO> {
        val sql = """
            SELECT spdi.person_ident, spdi.utbetaling_id, sykmelding.sykmelding_id FROM sykepengedager_informasjon spdi
            LEFT JOIN UTSENDT_VARSEL utsendt_varsel ON spdi.utbetaling_id = utsendt_varsel.utbetaling_id
            JOIN SYKMELDING sykmelding ON spdi.person_ident = sykmelding.employee_identification_number
            WHERE spdi.gjenstaende_sykedager < $gjenstaendeSykedagerLimit
            AND spdi.FORELOPIG_BEREGNET_SLUTT >= current_date + INTERVAL '$maxDateLimit' DAY
            AND sykmelding.tom > CURRENT_TIMESTAMP
            AND (utsendt_varsel.UTSENDT_TIDSPUNKT IS NULL OR utsendt_varsel.UTSENDT_TIDSPUNKT <= NOW() - INTERVAL '$nyttVarselLimit' DAY);
            """

        return namedParameterJdbcTemplate.query(sql, MerOppfolgingVarselDTOMapper())
    }
}

internal class UtsendtVarselRowMapper : RowMapper<UtsendtVarsel> {
    override fun mapRow(rs: ResultSet, rowNum: Int): UtsendtVarsel {
        return UtsendtVarsel(
            uuid = UUID.fromString(rs.getString("uuid")),
            personIdent = rs.getString("person_ident"),
            utsendtTidspunkt = rs.getTimestamp("utsendt_tidspunkt").toLocalDateTime(),
            utbetalingId = rs.getString("utbetaling_id"),
            sykmeldingId = rs.getString("sykmelding_id"),
        )
    }
}

internal class MerOppfolgingVarselDTOMapper : RowMapper<MerOppfolgingVarselDTO> {
    override fun mapRow(rs: ResultSet, rowNum: Int): MerOppfolgingVarselDTO {
        return MerOppfolgingVarselDTO(
            personIdent = rs.getString("person_ident"),
            utbetalingId = rs.getString("utbetaling_id"),
            sykmeldingId = rs.getString("sykmelding_id"),
        )
    }
}
