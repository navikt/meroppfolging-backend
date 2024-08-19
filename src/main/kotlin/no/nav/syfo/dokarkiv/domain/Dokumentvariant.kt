package no.nav.syfo.dokarkiv.domain

data class Dokumentvariant(
    val filtype: String,
    val variantformat: String,
    val fysiskDokument: ByteArray,
    val filnavn: String,
) {
    companion object {
        fun create(
            fysiskDokument: ByteArray,
            uuid: String,
        ): Dokumentvariant {
            return Dokumentvariant(
                filtype = "PDFA",
                variantformat = "ARKIV",
                fysiskDokument = fysiskDokument,
                filnavn = "Kvittering for snart slutt på sykepenger-$uuid",
            )
        }
    }
}
