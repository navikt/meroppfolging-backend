package no.nav.syfo.syfoopppdfgen

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenUtil
import no.nav.syfo.logger
import no.nav.syfo.maksdato.EsyfovarselClient
import no.nav.syfo.senoppfolging.v2.domain.FremtidigSituasjonSvar
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionV2
import no.nav.syfo.senoppfolging.v2.domain.behovForOppfolging
import no.nav.syfo.senoppfolging.v2.domain.fremtidigSituasjonSvar
import org.springframework.stereotype.Component

@Component
class PdfgenService(
    val syfooppfpdfgenClient: PdfgenClient,
    val esyfovarselClient: EsyfovarselClient,
    val tokenValidationContextHolder: TokenValidationContextHolder,
) {
    private val log = logger()

    private fun getKvitteringEndpoint(fremtidigSituasjonSvar: FremtidigSituasjonSvar): String {
        return when (fremtidigSituasjonSvar) {
            FremtidigSituasjonSvar.USIKKER -> "usikker_receipt"
            FremtidigSituasjonSvar.BYTTE_JOBB -> "bytte_jobb_receipt"
            FremtidigSituasjonSvar.FORTSATT_SYK -> "fortsatt_syk_receipt"
            FremtidigSituasjonSvar.TILBAKE_GRADERT -> "tilbake_gradert_receipt"
            FremtidigSituasjonSvar.TILBAKE_MED_TILPASNINGER -> "tilbake_med_tilpasninger_receipt"
            FremtidigSituasjonSvar.TILBAKE_HOS_ARBEIDSGIVER -> "tilbake_hos_arbeidsgiver_receipt"
            else -> {
                log.error("Could not map FremtidigSituasjonSvar type: $fremtidigSituasjonSvar")
                throw IllegalArgumentException("Invalid FremtidigSituasjonSvar type: $fremtidigSituasjonSvar")
            }
        }
    }

    fun getPdf(
        answersToQuestions: List<SenOppfolgingQuestionV2>,
    ): ByteArray? {
        val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, TokenUtil.TokenIssuer.TOKENX)
        val sykepengerMaxDateResponse = esyfovarselClient.getSykepengerMaxDateResponse(token)
        val behovForOppfolging = answersToQuestions.behovForOppfolging()
        val fremtidigSituasjonSvar = answersToQuestions.fremtidigSituasjonSvar()
        val kvitteringEndpoint = getKvitteringEndpoint(fremtidigSituasjonSvar)

        return syfooppfpdfgenClient.getSenOppfolgingPdf(
            kvitteringEndpoint = kvitteringEndpoint,
            behovForOppfolging = behovForOppfolging,
            daysUntilMaxDate = sykepengerMaxDateResponse?.gjenstaendeSykedager,
        )
    }
}
