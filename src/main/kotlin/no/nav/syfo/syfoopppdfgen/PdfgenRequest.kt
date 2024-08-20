package no.nav.syfo.syfoopppdfgen

data class PdfgenSenOppfolgingRequest(
    val senOppfolgingData: SenOppfolgingData,
)

class SenOppfolgingData(
    val daysUntilMaxDate: String?,
    val behovForOppfolging: Boolean,
    val sentDate: String,
)
