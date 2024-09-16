package no.nav.syfo.sykmelding.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class ReceivedSykmeldingDTO(
    val sykmelding: Sykmelding,
    val personNrPasient: String,
    val mottattDato: LocalDateTime,
)

data class Sykmelding(
    val id: String,
    val arbeidsgiver: Arbeidsgiver,
    val perioder: List<Periode>,
)

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
)

data class Arbeidsgiver(
    val harArbeidsgiver: HarArbeidsgiver,
)

enum class HarArbeidsgiver(
    val codeValue: String,
    val text: String,
    val oid: String = "2.16.578.1.12.4.1.1.8130"
) {
    EN_ARBEIDSGIVER("1", "Én arbeidsgiver"),
    FLERE_ARBEIDSGIVERE("2", "Flere arbeidsgivere"),
    INGEN_ARBEIDSGIVER("3", "Ingen arbeidsgiver")
}
