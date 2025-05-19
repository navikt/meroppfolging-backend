package no.nav.syfo.dokarkiv.domain

const val JOURNALFORENDE_ENHET = 9999 // automatisk journalføring uten mennesker involvert

data class DokarkivRequest(
    val avsenderMottaker: AvsenderMottaker,
    val tittel: String,
    val bruker: Bruker? = null,
    val dokumenter: List<Dokument>,
    val journalfoerendeEnhet: Int?,
    val journalpostType: String,
    val tema: String,
    val sak: Sak,
    val overstyrInnsynsregler: String,
    val eksternReferanseId: String,
    val kanal: String
) {
    companion object {
        fun create(
            avsenderMottaker: AvsenderMottaker,
            dokumenter: List<Dokument>,
            eksternReferanseId: String,
            tittel: String,
        ) = DokarkivRequest(
            avsenderMottaker = avsenderMottaker,
            tittel = tittel,
            bruker = Bruker.create(id = avsenderMottaker.id, idType = avsenderMottaker.idType),
            dokumenter = dokumenter,
            journalfoerendeEnhet = JOURNALFORENDE_ENHET,
            journalpostType = "INNGAAENDE",
            tema = "OPP", // Oppfolging
            sak = Sak("GENERELL_SAK"),
            // By default, user can not see documents created by others. Following enables viewing on Mine Saker:
            overstyrInnsynsregler = "VISES_MASKINELT_GODKJENT",
            eksternReferanseId = eksternReferanseId,
            kanal = "INGEN_DISTRIBUSJON"
        )
    }
}

data class AvsenderMottaker(
    val id: String,
    val idType: String,
) {
    companion object {
        fun create(
            id: String,
        ) = AvsenderMottaker(
            id = id,
            idType = "FNR",
        )
    }
}

data class Bruker(
    val id: String,
    val idType: String,
) {
    companion object {
        fun create(
            id: String,
            idType: String = "FNR",
        ) = Bruker(
            id = id,
            idType = idType,
        )
    }
}

data class Dokument(
    val brevkode: String,
    val dokumentvarianter: List<Dokumentvariant>,
    val tittel: String,
) {
    companion object {
        fun create(
            dokumentvarianter: List<Dokumentvariant>,
            tittel: String
        ) = Dokument(
            tittel = tittel,
            brevkode = "KVITTERING_SNART_SLUTT_PA_SYKEPENGER",
            dokumentvarianter = dokumentvarianter,
        )
    }
}

data class Sak(val sakstype: String = "GENERELL_SAK")
