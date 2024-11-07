package no.nav.syfo.syfoopppdfgen

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenUtil
import no.nav.syfo.auth.TokenUtil.TokenIssuer.TOKENX
import no.nav.syfo.dkif.DkifClient
import no.nav.syfo.maksdato.EsyfovarselClient
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionV2
import no.nav.syfo.senoppfolging.v2.domain.behovForOppfolging
import no.nav.syfo.senoppfolging.v2.domain.fremtidigSituasjonSvar
import no.nav.syfo.sykepengedagerinformasjon.database.SykepengedagerInformasjonDAO
import org.springframework.stereotype.Component

@Component
class PdfgenService(
    val syfooppfpdfgenClient: PdfgenClient,
    val esyfovarselClient: EsyfovarselClient,
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val dkifClient: DkifClient,
    val sykepengedagerInformasjonDAO: SykepengedagerInformasjonDAO,
) {

    fun getSenOppfolgingReceiptPdf(
        answersToQuestions: List<SenOppfolgingQuestionV2>,
    ): ByteArray? {
        val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, TOKENX)
        val sykepengerMaxDateResponse = esyfovarselClient.getSykepengerMaxDateResponse(token)
        val behovForOppfolging = answersToQuestions.behovForOppfolging()
        val fremtidigSituasjonSvar = answersToQuestions.fremtidigSituasjonSvar()

        return syfooppfpdfgenClient.createSenOppfolgingReceiptPdf(
            fremtidigSituasjonSvar = fremtidigSituasjonSvar,
            behovForOppfolging = behovForOppfolging,
            maxDate = sykepengerMaxDateResponse?.maxDate,
            daysUntilMaxDate = sykepengerMaxDateResponse?.gjenstaendeSykedager,
        )
    }

    fun getSenOppfolgingLandingPdf(personIdent: String): ByteArray {
        val isUserReservert = dkifClient.person(personIdent).kanVarsles == false
        val sykepengerInformasjon = sykepengedagerInformasjonDAO.fetchSykepengedagerInformasjonByFnr(personIdent)

        return when {
            isUserReservert -> syfooppfpdfgenClient.createSenOppfolgingLandingReservertPdf(
                utbetaltTom = sykepengerInformasjon?.utbetaltTom.toString(),
                maxDate = sykepengerInformasjon?.forelopigBeregnetSlutt.toString(),
            )

            else -> syfooppfpdfgenClient.createSenOppfolgingLandingPdf(
                daysLeft = sykepengerInformasjon?.gjenstaendeSykedager.toString(),
                utbetaltTom = sykepengerInformasjon?.utbetaltTom.toString(),
                maxDate = sykepengerInformasjon?.forelopigBeregnetSlutt.toString(),
            )
        }
    }
}
