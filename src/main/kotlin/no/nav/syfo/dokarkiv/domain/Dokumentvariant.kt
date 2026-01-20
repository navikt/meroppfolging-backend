package no.nav.syfo.dokarkiv.domain

data class Dokumentvariant(
    val filtype: String,
    val variantformat: String,
    val fysiskDokument: ByteArray,
    val filnavn: String,
) {
    companion object {
        fun create(fysiskDokument: ByteArray, filnavn: String,): Dokumentvariant = Dokumentvariant(
            filtype = "PDFA",
            variantformat = "ARKIV",
            fysiskDokument = fysiskDokument,
            filnavn = filnavn,
        )
    }
}
