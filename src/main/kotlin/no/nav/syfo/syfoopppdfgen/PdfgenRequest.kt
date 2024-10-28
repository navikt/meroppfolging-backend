package no.nav.syfo.syfoopppdfgen

data class PdfgenRequest(
    val brevdata: Brevdata,
)

interface Brevdata

class BrevdataSenOppfolging(
    val sentDate: String,
    val behovForOppfolging: Boolean,
    val maxdato: String?,
    val daysUntilMaxDate: String?,
) : Brevdata

class BrevdataMerVeiledning(
    val sendtdato: String,
    val utbetaltTom: String?,
    val maxdato: String?,
) : Brevdata

class BrevdataMerVeiledningPilot(
    val sendtdato: String,
    val daysLeft: String?,
    val maxdato: String?,
    val utbetaltTom: String?,
) : Brevdata
