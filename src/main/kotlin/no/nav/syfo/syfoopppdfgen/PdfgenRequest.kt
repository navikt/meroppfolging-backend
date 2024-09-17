package no.nav.syfo.syfoopppdfgen

data class PdfgenRequest(
    val brevdata: Brevdata,
)

class Brevdata(
    val daysUntilMaxDate: String?,
    val behovForOppfolging: Boolean,
    val sentDate: String,
)
