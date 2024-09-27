package no.nav.syfo.sykepengedagerinformasjon.database

import no.nav.syfo.sykepengedagerinformasjon.domain.PSykepengedagerInformasjon
import no.nav.syfo.sykepengedagerinformasjon.domain.SykepengedagerInformasjonDTO
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class SykepengedagerInformasjonDAO(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,) {
    fun persistSykepengedagerInformasjon(sykepengeDagerDTO: SykepengedagerInformasjonDTO) {
        val insertStatement = """
            INSERT INTO SYKEPENGEDAGER_INFORMASJON (
                utbetaling_id,
                person_ident,
                forelopig_beregnet_slutt,
                utbetalt_tom,
                gjenstaende_sykedager,
                utbetaling_created_at,
                received_at
            ) VALUES (:utbetaling_id, :person_ident, :forelopig_beregnet_slutt, :utbetalt_tom, :gjenstaende_sykedager, :utbetaling_created_at, :received_at)
            ;
        """.trimIndent()

        val parameters = MapSqlParameterSource()
            .addValue("utbetaling_id", sykepengeDagerDTO.id)
            .addValue("person_ident", sykepengeDagerDTO.personIdent)
            .addValue(
                "forelopig_beregnet_slutt",
                Timestamp.valueOf(sykepengeDagerDTO.forelopigBeregnetSlutt.atStartOfDay())
            )
            .addValue("utbetalt_tom", Timestamp.valueOf(sykepengeDagerDTO.utbetaltTom.atStartOfDay()))
            .addValue("gjenstaende_sykedager", sykepengeDagerDTO.personIdent)
            .addValue("utbetaling_created_at", Timestamp.valueOf(sykepengeDagerDTO.createdAt))
            .addValue("received_at", Timestamp.valueOf(LocalDateTime.now()))

        namedParameterJdbcTemplate.update(insertStatement, parameters)
    }

    fun fetchSykepengedagerInformasjon(
        utbetalingId: String,
    ): PSykepengedagerInformasjon? {
        val selectStatement = """
        SELECT *
        FROM SYKEPENGEDAGER_INFORMASJON
        WHERE utbetaling_id = :utbetalingId
        """.trimIndent()

        val parameters = MapSqlParameterSource()
            .addValue("utbetaling_id", utbetalingId)

        return namedParameterJdbcTemplate.query(selectStatement, parameters) { rs, _ -> rs.toPSykmelding() }
            .firstOrNull()
    }

    fun ResultSet.toPSykmelding() = PSykepengedagerInformasjon(
        utbetalingId = getString("utbetaling_id"),
        personIdent = getString("person_ident"),
        forelopigBeregnetSlutt = getTimestamp("forelopig_beregnet_slutt").toLocalDateTime().toLocalDate(),
        utbetaltTom = getTimestamp("utbetalt_tom").toLocalDateTime().toLocalDate(),
        gjenstaendeSykedager = getString("gjenstaende_sykedager"),
        utbetalingCreatedAt = getTimestamp("utbetaling_created_at").toLocalDateTime(),
        receivedAt = getTimestamp("received_at").toLocalDateTime(),
    )
}
