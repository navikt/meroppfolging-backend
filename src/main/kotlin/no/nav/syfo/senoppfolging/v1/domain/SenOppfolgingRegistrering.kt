package no.nav.syfo.senoppfolging.v1.domain

// DTO'er som brukes mot veilarbregistrering, må ikke endres
data class SenOppfolgingRegistrering(
    val besvarelse: Besvarelse,
    val teksterForBesvarelse: List<TekstForSporsmal>,
)

data class Besvarelse(
    val utdanning: UtdanningSvar? = null,
    val utdanningBestatt: UtdanningBestattSvar? = null,
    val utdanningGodkjent: UtdanningGodkjentSvar? = null,
    val andreForhold: AndreForholdSvar? = null,
    val sisteStilling: SisteStillingSvar? = null,
    val fremtidigSituasjon: FremtidigSituasjonSvar? = null,
    val tilbakeIArbeid: TilbakeIArbeidSvar? = null,
)

data class TekstForSporsmal(
    val sporsmalId: String,
    val sporsmal: String,
    val svar: String,
)

enum class UtdanningSvar {
    INGEN_UTDANNING,
    GRUNNSKOLE,
    VIDEREGAENDE_GRUNNUTDANNING,
    VIDEREGAENDE_FAGBREV_SVENNEBREV,
    HOYERE_UTDANNING_1_TIL_4,
    HOYERE_UTDANNING_5_ELLER_MER,
    INGEN_SVAR,
}

enum class UtdanningBestattSvar {
    JA,
    NEI,
    INGEN_SVAR,
}

enum class UtdanningGodkjentSvar {
    JA,
    NEI,
    VET_IKKE,
    INGEN_SVAR,
}

enum class AndreForholdSvar {
    JA,
    NEI,
    INGEN_SVAR,
}

enum class FremtidigSituasjonSvar {
    SAMME_ARBEIDSGIVER,
    SAMME_ARBEIDSGIVER_NY_STILLING,
    NY_ARBEIDSGIVER,
    USIKKER,
    INGEN_PASSER,
}

enum class TilbakeIArbeidSvar {
    JA_FULL_STILLING,
    JA_REDUSERT_STILLING,
    USIKKER,
    NEI,
}

enum class SisteStillingSvar {
    HAR_HATT_JOBB,
    HAR_IKKE_HATT_JOBB,
    INGEN_SVAR,
}
