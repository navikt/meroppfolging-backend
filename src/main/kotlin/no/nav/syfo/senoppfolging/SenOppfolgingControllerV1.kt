package no.nav.syfo.senoppfolging

import jakarta.annotation.PostConstruct
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenUtil
import no.nav.syfo.auth.TokenUtil.TokenIssuer.TOKENX
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.besvarelse.database.ResponseDao
import no.nav.syfo.besvarelse.database.domain.FormType
import no.nav.syfo.besvarelse.database.domain.QuestionResponse
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.domain.senoppfolging.SenOppfolgingQuestionTypeV1
import no.nav.syfo.domain.senoppfolging.SenOppfolgingQuestionTypeV1.ANDRE_FORHOLD
import no.nav.syfo.domain.senoppfolging.SenOppfolgingQuestionTypeV1.FREMTIDIG_SITUASJON
import no.nav.syfo.domain.senoppfolging.SenOppfolgingQuestionTypeV1.SISTE_STILLING
import no.nav.syfo.domain.senoppfolging.SenOppfolgingQuestionTypeV1.TILBAKE_I_ARBEID
import no.nav.syfo.domain.senoppfolging.SenOppfolgingQuestionTypeV1.UTDANNING
import no.nav.syfo.domain.senoppfolging.SenOppfolgingQuestionTypeV1.UTDANNING_BESTATT
import no.nav.syfo.domain.senoppfolging.SenOppfolgingQuestionTypeV1.UTDANNING_GODKJENT
import no.nav.syfo.logger
import no.nav.syfo.metric.Metric
import no.nav.syfo.oppfolgingstilfelle.IsOppfolgingstilfelleClient
import no.nav.syfo.senoppfolging.domain.SenOppfolgingRegistrering
import no.nav.syfo.senoppfolging.domain.StatusDTO
import no.nav.syfo.senoppfolging.domain.TekstForSporsmal
import no.nav.syfo.veilarbregistrering.VeilarbregistreringClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping("/api/v1/senoppfolging")
@ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
class SenOppfolgingControllerV1(
    @Value("\${MEROPPFOLGING_FRONTEND_CLIENT_ID}")
    val merOppfolgingFrontendClientId: String,
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val veilarbregistreringClient: VeilarbregistreringClient,
    val isOppfolgingstilfelleClient: IsOppfolgingstilfelleClient,
    val metric: Metric,
    val responseDao: ResponseDao,
) {
    lateinit var tokenValidator: TokenValidator
    private val log = logger()

    @PostConstruct
    fun init() {
        tokenValidator = TokenValidator(tokenValidationContextHolder, merOppfolgingFrontendClientId)
    }

    @GetMapping("/status", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun status(): StatusDTO {
        tokenValidator.validateTokenXClaims()
        val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, TOKENX)
        val startRegistration = veilarbregistreringClient.startRegistration(token)
        val isSykmeldt = isOppfolgingstilfelleClient.isSykmeldt(token)
        log.info(
            "veilarbregistrering type [${startRegistration.registreringType},${startRegistration.formidlingsgruppe}," +
                "${startRegistration.servicegruppe},${startRegistration.rettighetsgruppe},$isSykmeldt]",
        )
        return StatusDTO(startRegistration.registreringType, isSykmeldt)
    }

    @PostMapping("/submit")
    @ResponseBody
    fun submit(
        @RequestBody senOppfolgingRegistrering: SenOppfolgingRegistrering,
    ) {
        val claims = tokenValidator.validateTokenXClaims()
        val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, TOKENX)
        veilarbregistreringClient.completeRegistration(token, senOppfolgingRegistrering)

        responseDao.saveFormResponse(
            PersonIdentNumber(tokenValidator.getFnrFromIdportenTokenX(claims)),
            senOppfolgingRegistrering.toQuestions(),
            FormType.SEN_OPPFOLGING_V1,
        )

        metric.countSenOppfolgingSubmitted()
    }

    fun SenOppfolgingRegistrering.toQuestions(): List<QuestionResponse> {
        return listOf(
            question(UTDANNING, besvarelse.utdanning.name),
            question(UTDANNING_BESTATT, besvarelse.utdanningBestatt.name),
            question(UTDANNING_GODKJENT, besvarelse.utdanningGodkjent.name),
            question(ANDRE_FORHOLD, besvarelse.andreForhold.name),
            question(SISTE_STILLING, besvarelse.sisteStilling.name),
            question(FREMTIDIG_SITUASJON, besvarelse.fremtidigSituasjon.name),
            question(TILBAKE_I_ARBEID, besvarelse.tilbakeIArbeid.name),
        )
    }

    fun SenOppfolgingRegistrering.question(
        type: SenOppfolgingQuestionTypeV1,
        answerType: String,
    ): QuestionResponse {
        return QuestionResponse(
            type.name,
            teksterForBesvarelse.tekstForSporsmal(type).sporsmal,
            answerType,
            teksterForBesvarelse.tekstForSporsmal(type).svar,
        )
    }

    fun List<TekstForSporsmal>.tekstForSporsmal(sporsmalType: SenOppfolgingQuestionTypeV1): TekstForSporsmal {
        return find { it.sporsmalId == sporsmalType.name }
            ?: throw IllegalStateException("Couldn't find question $sporsmalType")
    }
}
