package no.nav.syfo.syfoopppdfgen

data class PdfgenRequest(
    val brevdata: Brevdata,
)

interface Brevdata

class BrevdataSenOppfolgingReceipt(
    val sentDate: String,
    val behovForOppfolging: Boolean,
    val questionTextFremtidigSituasjon: String?,
    val answerTextFremtidigSituasjon: String?,
    val questionTextBehovForOppfolging: String?,
    val answerTextBehovForOppfolging: String?,
    val submittedDateFormatted: String,
    val maxdato: String?,
    val daysUntilMaxDate: String?,
) : Brevdata

class BrevdataSenOppfolgingLandingReservert(
    val sendtdato: String,
    val utbetaltTom: String?,
    val maxdato: String?,
) : Brevdata

class BrevdataSenOppfolgingLanding(
    val sendtdato: String,
    val daysLeft: String?,
    val maxdato: String?,
    val utbetaltTom: String?,
) : Brevdata
