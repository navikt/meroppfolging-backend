package no.nav.syfo.sykmelding.database

import no.nav.syfo.sykmelding.domain.Sykmeldingsperiode
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class SykmeldingDao(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun persistSykmeldingsperiode(
        sykmeldingId: String,
        employeeIdentificationNumber: String,
        fom: LocalDate,
        tom: LocalDate,
        harArbeidsgiver: Boolean,
    ) {
        val insertStatement = """
            INSERT INTO SYKMELDINGSPERIODE (
                sykmelding_id,
                has_employer,
                employee_identification_number,
                fom,
                tom,
                created_at
            ) VALUES (:sykmelding_id, :has_employer, :employee_identification_number, :fom, :tom, :created_at)
            ON CONFLICT (sykmelding_id) DO NOTHING
        """.trimIndent()

        val parameters = MapSqlParameterSource()
            .addValue("sykmelding_id", sykmeldingId)
            .addValue("has_employer", harArbeidsgiver)
            .addValue("employee_identification_number", employeeIdentificationNumber)
            .addValue("fom", Timestamp.valueOf(fom.atStartOfDay()))
            .addValue("tom", Timestamp.valueOf(tom.atStartOfDay()))
            .addValue("created_at", Timestamp.valueOf(LocalDateTime.now()))

        namedParameterJdbcTemplate.update(insertStatement, parameters)
    }

    fun deleteSykmeldingsperioder(sykmeldingId: String) {
        val deleteStatement = """
        DELETE FROM SYKMELDINGSPERIODE
        WHERE sykmelding_id = :sykmelding_id
        """.trimIndent()

        val parameters = MapSqlParameterSource()
            .addValue("sykmelding_id", sykmeldingId)

        namedParameterJdbcTemplate.update(deleteStatement, parameters)
    }

    fun getSykmeldingsperioder(
        employeeIdentificationNumber: String,
    ): List<Sykmeldingsperiode> {
        val selectStatement = """
        SELECT *
        FROM SYKMELDINGSPERIODE
        WHERE employee_identification_number = :employee_identification_number
        """.trimIndent()

        val parameters = MapSqlParameterSource()
            .addValue("employee_identification_number", employeeIdentificationNumber)

        return namedParameterJdbcTemplate.query(selectStatement, parameters) { rs, _ -> rs.toSykmeldingsperiode() }
    }

    fun ResultSet.toSykmeldingsperiode() = Sykmeldingsperiode(
        sykmeldingId = getString("sykmelding_id"),
        hasEmployer = getBoolean("has_employer"),
        employeeIdentificationNumber = getString("employee_identification_number"),
        fom = getTimestamp("fom").toLocalDateTime().toLocalDate(),
        tom = getTimestamp("tom").toLocalDateTime().toLocalDate(),
        createdAt = getTimestamp("created_at").toLocalDateTime(),
    )
}
