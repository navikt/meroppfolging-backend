package no.nav.syfo.syfoopppdfgen

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenUtil
import no.nav.syfo.auth.TokenUtil.TokenIssuer.TOKENX
import no.nav.syfo.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.behandlendeenhet.domain.isPilot
import no.nav.syfo.dkif.DkifClient
import no.nav.syfo.logger
import no.nav.syfo.maksdato.EsyfovarselClient
import no.nav.syfo.senoppfolging.v2.domain.FremtidigSituasjonSvar
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionV2
import no.nav.syfo.senoppfolging.v2.domain.behovForOppfolging
import no.nav.syfo.senoppfolging.v2.domain.fremtidigSituasjonSvar
import no.nav.syfo.sykepengedagerinformasjon.database.SykepengedagerInformasjonDAO
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
    val sykepengedagerInformasjonDAO: SykepengedagerInformasjonDAO,
) {
    private val log = logger()

    private val urlForReservedUsers = "/oppfolging/mer_veiledning_for_reserverte"
    private val urlForDigitalUsers = "/oppfolging/mer_veiledning_for_digitale"
    private val urlForDigitalPilotUsers = "/senoppfolging/landing"

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

    fun getMerVeiledningPdf(personIdent: String): ByteArray {
        val behandlendeEnhet = behandlendeEnhetClient.getBehandlendeEnhet(personIdent)
        val isPilotUser = behandlendeEnhet.isPilot(clusterName)
        val isUserReservert = dkifClient.person(personIdent)?.kanVarsles == true
        val sykepengerInformasjon = sykepengedagerInformasjonDAO.fetchSykepengedagerInformasjonByFnr(personIdent)

        return when {
            isUserReservert -> syfooppfpdfgenClient.getMerVeiledningPdf(
                pdfEndpoint = urlForReservedUsers,
                utbetaltTom = sykepengerInformasjon?.utbetaltTom.toString(),
                maxDate = sykepengerInformasjon?.forelopigBeregnetSlutt.toString(),
            )

            isPilotUser -> syfooppfpdfgenClient.getMerVeiledningPilotUserPdf(
                pdfEndpoint = urlForDigitalPilotUsers,
                // sykepengerInformasjon.receivedAt> "prodsettingstidspunkt"
                daysLeft = sykepengerInformasjon?.gjenstaendeSykedager.toString(),
                maxDate = sykepengerInformasjon?.forelopigBeregnetSlutt.toString(),
            )

            else -> syfooppfpdfgenClient.getMerVeiledningPdf(
                pdfEndpoint = urlForDigitalUsers,
                utbetaltTom = sykepengerInformasjon?.utbetaltTom.toString(),
                maxDate = sykepengerInformasjon?.forelopigBeregnetSlutt.toString(),
            )
        }
    }
}
