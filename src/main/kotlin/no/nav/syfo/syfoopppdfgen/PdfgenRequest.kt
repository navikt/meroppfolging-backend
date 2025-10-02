package no.nav.syfo.syfoopppdfgen

import no.nav.syfo.kartlegging.domain.formsnapshot.FieldSnapshot
import no.nav.syfo.senoppfolging.v2.domain.BehovForOppfolgingSvar
import no.nav.syfo.senoppfolging.v2.domain.FremtidigSituasjonSvar

data class PdfgenRequest(
    val brevdata: Brevdata,
)

interface Brevdata

class BrevdataSenOppfolgingReceipt(
    val behovForOppfolging: Boolean,
    val questionTextFremtidigSituasjon: String?,
    val answerTextFremtidigSituasjon: String?,
    val questionTextBehovForOppfolging: String?,
    val answerTextBehovForOppfolging: String?,
    val submissionDateFormatted: String,
    val maxdatoFormatted: String?,
    val utbetaltTomFormatted: String?,
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

class BrevdataSenOppfolgingFormSteps(
    val fremtidigSituasjonAnswer: FremtidigSituasjonSvar?,
    val behovForOppfolgingAnswer: BehovForOppfolgingSvar?,
) : Brevdata

data class KartleggingPdfgenRequest(
    val createdAt: String,
    val fieldSnapshots: List<FieldSnapshot>,
): Brevdata
