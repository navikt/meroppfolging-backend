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
    val upperBoundDagerIgjenTilMaksdatoForVarsling = 120
    val lowerBoundDagerIgjenTilMaksdatoForVarsling = 14
    val maxDaysSinceUtbetalingCreatedForTakingItIntoAccount = 60
    val minDagerMellomToVarslerTilSammePerson = 110

    fun getUtsendtVarsel(personIdent: String): UtsendtVarsel? {
        val sql =
            """
            select uuid, person_ident, utsendt_tidspunkt, utbetaling_id, sykmelding_id
            from UTSENDT_VARSEL
            where person_ident = :person_ident
            AND UTSENDT_TIDSPUNKT > NOW() - INTERVAL '$minDagerMellomToVarslerTilSammePerson' DAY
            """.trimIndent()

        val parameters =
            mapOf(
                "person_ident" to personIdent,
            )

        return namedParameterJdbcTemplate.query(sql, parameters, UtsendtVarselRowMapper()).firstOrNull()
    }

    fun getUtsendtVarselFromEsyfovarselCopy(personIdent: String): UtsendtVarselEsyfovarselCopy? {
        val sql =
            """
            select uuid_esyfovarsel, fnr, utsendt_tidspunkt
            from COPY_UTSENDT_VARSEL_ESYFOVARSEL
            where fnr = :person_ident
            AND utsendt_tidspunkt > NOW() - INTERVAL '$minDagerMellomToVarslerTilSammePerson' DAY
            """.trimIndent()

        val parameters =
            mapOf(
                "person_ident" to personIdent,
            )

        return namedParameterJdbcTemplate.query(sql, parameters, UtsendtVarselEsyfovarselCopyRowMapper()).firstOrNull()
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
            WITH filtrerte_utbetalinger AS (
                SELECT spdi.utbetaling_id,
                       spdi.person_ident,
                       spdi.gjenstaende_sykedager,
                       spdi.forelopig_beregnet_slutt,
                       spdi.utbetaling_created_at,
                       sykmelding.sykmelding_id
                FROM sykepengedager_informasjon AS spdi
                         JOIN SYKMELDING sykmelding ON spdi.person_ident = sykmelding.employee_identification_number
                -- only consider latest utbetaling
                WHERE spdi.utbetaling_id =
                      (SELECT spdi2.utbetaling_id
                       FROM sykepengedager_informasjon AS spdi2
                       WHERE spdi.person_ident = spdi2.person_ident
                       ORDER BY utbetaling_created_at DESC
                       LIMIT 1)
                  -- only consider latest sykmelding
                  AND sykmelding.sykmelding_id =
                      (SELECT latest_sykmelding.sykmelding_id
                       FROM sykmelding AS latest_sykmelding
                       WHERE sykmelding.employee_identification_number = latest_sykmelding.employee_identification_number
                       ORDER BY latest_sykmelding.created_at DESC
                       LIMIT 1)
                  -- person must be currently sykmeldt
                  AND sykmelding.tom > CURRENT_TIMESTAMP
                  -- check that we are within range of maksdato for the latest utbetaling
                  AND spdi.forelopig_beregnet_slutt <= current_date + INTERVAL '120' DAY
                  AND spdi.forelopig_beregnet_slutt >= current_date + INTERVAL '14' DAY
                  -- check that the utbetaling is not too old to consider, unless it has a low number of gjenstaende_sykedager
                  AND (spdi.utbetaling_created_at >= NOW() - INTERVAL '60' DAY
                    OR
                       spdi.gjenstaende_sykedager < 85)
                  -- check that we have waited long enough since the person last got a SSPS-varsel
                  AND spdi.person_ident NOT IN
                      (SELECT utsendt_varsel.person_ident
                       FROM UTSENDT_VARSEL
                       WHERE UTSENDT_TIDSPUNKT > NOW() - INTERVAL '110' DAY)
                  AND spdi.person_ident NOT IN
                      (SELECT copy_utsendt_varsel_esyfovarsel.fnr
                       FROM copy_utsendt_varsel_esyfovarsel
                       WHERE UTSENDT_TIDSPUNKT > NOW() - INTERVAL '110' DAY)
                  -- check if we should skip sending a varsel to the person
                  AND spdi.person_ident NOT IN
                      (SELECT skip_varselutsending.person_ident
                       FROM skip_varselutsending)
            )
            SELECT *
            FROM filtrerte_utbetalinger LEFT JOIN LATERAL (
                SELECT employee_identification_number,
                       -- count number of sykmelding days since utbetaling_created_at
                       SUM((tom::date - GREATEST(fom, filtrerte_utbetalinger.utbetaling_created_at)::date)::int + 1)
                           AS sykmelding_days
                FROM sykmelding
                GROUP BY employee_identification_number) sd
            ON filtrerte_utbetalinger.person_ident = sd.employee_identification_number
            WHERE NOT (
                -- If there is more than a month since the utbetaling was created, check that the person has been
                -- sykmeldt more than half of the days since the utbetaling was created.
                filtrerte_utbetalinger.utbetaling_created_at < (CURRENT_DATE - INTERVAL '1 month')
                    AND COALESCE(sd.sykmelding_days, 0) <
                        (DATE_PART('day', CURRENT_DATE - filtrerte_utbetalinger.utbetaling_created_at) / 2)
                );
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

internal class UtsendtVarselEsyfovarselCopyRowMapper : RowMapper<UtsendtVarselEsyfovarselCopy> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): UtsendtVarselEsyfovarselCopy =
        UtsendtVarselEsyfovarselCopy(
            uuid = UUID.fromString(rs.getString("uuid_esyfovarsel")),
            personIdent = rs.getString("fnr"),
            utsendtTidspunkt = rs.getTimestamp("utsendt_tidspunkt").toLocalDateTime(),
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
