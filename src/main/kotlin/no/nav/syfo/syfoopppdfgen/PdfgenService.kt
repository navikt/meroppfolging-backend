package no.nav.syfo.syfoopppdfgen

import no.nav.syfo.dkif.DkifClient
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionV2
import no.nav.syfo.senoppfolging.v2.domain.behovForOppfolging
import no.nav.syfo.senoppfolging.v2.domain.fremtidigSituasjonSvar
import no.nav.syfo.sykepengedagerinformasjon.domain.forelopigBeregnetSluttFormattedForLetter
import no.nav.syfo.sykepengedagerinformasjon.domain.utbetaltTomFormattedForLetter
import no.nav.syfo.sykepengedagerinformasjon.service.SykepengedagerInformasjonService
import org.springframework.stereotype.Component

@Component
class PdfgenService(
    val syfooppfpdfgenClient: PdfgenClient,
    val dkifClient: DkifClient,
    val sykepengedagerInformasjonService: SykepengedagerInformasjonService,
) {

    fun getSenOppfolgingReceiptPdf(
        personIdent: String,
        answersToQuestions: List<SenOppfolgingQuestionV2>,
    ): ByteArray? {
        val behovForOppfolging = answersToQuestions.behovForOppfolging()
        val fremtidigSituasjonSvar = answersToQuestions.fremtidigSituasjonSvar()
        val sykepengerInformasjon = sykepengedagerInformasjonService.fetchSykepengedagerInformasjonByIdent(
            personIdent
        )

        return syfooppfpdfgenClient.createSenOppfolgingReceiptPdf(
            fremtidigSituasjonSvar = fremtidigSituasjonSvar,
            behovForOppfolging = behovForOppfolging,
            maxDate = sykepengerInformasjon?.forelopigBeregnetSluttFormattedForLetter(),
            daysUntilMaxDate = sykepengerInformasjon?.gjenstaendeSykedager.toString(),
        )
    }

    fun getSenOppfolgingLandingPdf(personIdent: String): ByteArray {
        val isUserReservert = dkifClient.person(personIdent).kanVarsles == false
        val sykepengerInformasjon = sykepengedagerInformasjonService.fetchSykepengedagerInformasjonByIdent(
            personIdent
        )

        return syfooppfpdfgenClient.createSenOppfolgingLandingPdf(
            daysLeft = sykepengerInformasjon?.gjenstaendeSykedager.toString(),
            utbetaltTom = sykepengerInformasjon?.utbetaltTomFormattedForLetter(),
            maxDate = sykepengerInformasjon?.forelopigBeregnetSluttFormattedForLetter(),
            isForReservertUser = isUserReservert,
        )
    }
}
