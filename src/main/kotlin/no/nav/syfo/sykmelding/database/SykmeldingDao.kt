package no.nav.syfo.sykmelding.database

import no.nav.syfo.sykmelding.domain.PSykmelding
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class SykmeldingDao(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,) {
    fun persistSykmelding(sykmeldingId: String, employeeIdentificationNumber: String, fom: LocalDate, tom: LocalDate,) {
        val insertStatement = """
            INSERT INTO SYKMELDING (
                sykmelding_id,
                employee_identification_number,
                fom,
                tom,
                created_at
            ) VALUES (:sykmelding_id, :employee_identification_number, :fom, :tom, :created_at)
            ON CONFLICT (sykmelding_id) DO UPDATE SET
                employee_identification_number = EXCLUDED.employee_identification_number,
                fom = EXCLUDED.fom,
                tom = EXCLUDED.tom
            ;
        """.trimIndent()

        val parameters = MapSqlParameterSource()
            .addValue("sykmelding_id", sykmeldingId)
            .addValue("employee_identification_number", employeeIdentificationNumber)
            .addValue("fom", Timestamp.valueOf(fom.atStartOfDay()))
            .addValue("tom", Timestamp.valueOf(tom.atStartOfDay()))
            .addValue("created_at", Timestamp.valueOf(LocalDateTime.now()))

        namedParameterJdbcTemplate.update(insertStatement, parameters)
    }

    fun deleteSykmelding(sykmeldingId: String) {
        val deleteStatement = """
        DELETE FROM SYKMELDING
        WHERE sykmelding_id = :sykmelding_id
        """.trimIndent()

        val parameters = MapSqlParameterSource()
            .addValue("sykmelding_id", sykmeldingId)

        namedParameterJdbcTemplate.update(deleteStatement, parameters)
    }

    fun getSykmelding(employeeIdentificationNumber: String,): PSykmelding? {
        val selectStatement = """
        SELECT *
        FROM SYKMELDING
        WHERE employee_identification_number = :employee_identification_number
        ORDER BY created_at DESC
        """.trimIndent()

        val parameters = MapSqlParameterSource()
            .addValue("employee_identification_number", employeeIdentificationNumber)

        return namedParameterJdbcTemplate.query(selectStatement, parameters) { rs, _ -> rs.toPSykmelding() }
            .firstOrNull()
    }

    fun ResultSet.toPSykmelding() = PSykmelding(
        sykmeldingId = getString("sykmelding_id"),
        employeeIdentificationNumber = getString("employee_identification_number"),
        fom = getTimestamp("fom").toLocalDateTime().toLocalDate(),
        tom = getTimestamp("tom").toLocalDateTime().toLocalDate(),
        createdAt = getTimestamp("created_at").toLocalDateTime(),
    )
}
