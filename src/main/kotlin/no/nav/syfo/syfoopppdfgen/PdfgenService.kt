package no.nav.syfo.syfoopppdfgen

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenUtil
import no.nav.syfo.auth.TokenUtil.TokenIssuer.TOKENX
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.auth.getFnr
import no.nav.syfo.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.behandlendeenhet.domain.isPilot
import no.nav.syfo.dkif.DkifClient
import no.nav.syfo.logger
import no.nav.syfo.maksdato.EsyfovarselClient
import no.nav.syfo.senoppfolging.v2.domain.FremtidigSituasjonSvar
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionV2
import no.nav.syfo.senoppfolging.v2.domain.behovForOppfolging
import no.nav.syfo.senoppfolging.v2.domain.fremtidigSituasjonSvar
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class PdfgenService(
    val syfooppfpdfgenClient: PdfgenClient,
    val esyfovarselClient: EsyfovarselClient,
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val behandlendeEnhetClient: BehandlendeEnhetClient,
    val dkifClient: DkifClient,
    @Value("\${NAIS_CLUSTER_NAME}") private var clusterName: String,
) {
    lateinit var tokenValidator: TokenValidator
    private val log = logger()
    val isProd = "prod-gcp" == clusterName

    private val urlForReservedUsers = "/api/v1/genpdf/oppfolging/mer_veiledning_for_reserverte"
    private val urlForDigitalUsers = "/api/v1/genpdf/oppfolging/mer_veiledning_for_digitale"
    private val urlForDigitalPilotUsers = "/api/v1/genpdf/senoppfolging/landing"

    private fun getSenOppfolgingKvitteringEndpoint(fremtidigSituasjonSvar: FremtidigSituasjonSvar): String {
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

    fun getSenOppfolgingPdf(
        answersToQuestions: List<SenOppfolgingQuestionV2>,
    ): ByteArray? {
        val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, TOKENX)
        val sykepengerMaxDateResponse = esyfovarselClient.getSykepengerMaxDateResponse(token)
        val behovForOppfolging = answersToQuestions.behovForOppfolging()
        val fremtidigSituasjonSvar = answersToQuestions.fremtidigSituasjonSvar()
        val kvitteringEndpoint = getSenOppfolgingKvitteringEndpoint(fremtidigSituasjonSvar)

        return syfooppfpdfgenClient.getSenOppfolgingPdf(
            kvitteringEndpoint = kvitteringEndpoint,
            behovForOppfolging = behovForOppfolging,
            daysUntilMaxDate = sykepengerMaxDateResponse?.gjenstaendeSykedager,
        )
    }

    fun getMerVeiledningPdf(): ByteArray {
        val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, TOKENX)
        val sykepengerMaxDateResponse = esyfovarselClient.getSykepengerMaxDateResponse(token)
        val personIdent = tokenValidator.validateTokenXClaims().getFnr()
        val behandlendeEnhet = behandlendeEnhetClient.getBehandlendeEnhet(personIdent)
        val isPilotUser = behandlendeEnhet.isPilot(isProd = isProd)
        val isUserReservert = dkifClient.person(personIdent)?.kanVarsles == true

        return when {
            isUserReservert -> syfooppfpdfgenClient.getMerVeiledningPdf(
                pdfEndpoint = urlForReservedUsers,
                utbetaltTom = sykepengerMaxDateResponse?.utbetaltTom,
                maxDate = sykepengerMaxDateResponse?.maxDate
            )
            isPilotUser -> syfooppfpdfgenClient.getMerVeiledningPilotUserPdf(
                pdfEndpoint = urlForDigitalPilotUsers,
                daysLeft = sykepengerMaxDateResponse?.gjenstaendeSykedager,
                maxDate = sykepengerMaxDateResponse?.maxDate
            )
            else -> syfooppfpdfgenClient.getMerVeiledningPdf(
                pdfEndpoint = urlForDigitalUsers,
                utbetaltTom = sykepengerMaxDateResponse?.utbetaltTom,
                maxDate = sykepengerMaxDateResponse?.maxDate
            )
        }
    }
}
