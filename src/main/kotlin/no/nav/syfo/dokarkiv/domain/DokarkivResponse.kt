package no.nav.syfo.dokarkiv.domain

data class DokarkivResponse(
    val dokumenter: List<DokumentInfo>? = null,
    val journalpostId: String,
    val journalpostferdigstilt: Boolean? = null,
    val journalstatus: String,
    val melding: String? = null,
)
