package no.nav.syfo.senoppfolging

import jakarta.annotation.PostConstruct
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenUtil
import no.nav.syfo.auth.TokenUtil.TokenIssuer.TOKENX
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.auth.getFnr
import no.nav.syfo.besvarelse.database.ResponseDao
import no.nav.syfo.besvarelse.database.domain.FormType
import no.nav.syfo.besvarelse.database.domain.QuestionResponse
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.logger
import no.nav.syfo.metric.Metric
import no.nav.syfo.oppfolgingstilfelle.IsOppfolgingstilfelleClient
import no.nav.syfo.senoppfolging.domain.OnskerOppfolgingSvar
import no.nav.syfo.senoppfolging.domain.SenOppfolgingDTOV1
import no.nav.syfo.senoppfolging.domain.SenOppfolgingQuestionTypeV1
import no.nav.syfo.senoppfolging.domain.SenOppfolgingQuestionV1
import no.nav.syfo.senoppfolging.domain.SenOppfolgingRegistrering
import no.nav.syfo.senoppfolging.domain.StatusDTO
import no.nav.syfo.varsel.VarselService
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
@Suppress("LongParameterList")
class SenOppfolgingControllerV1(
    @Value("\${MEROPPFOLGING_FRONTEND_CLIENT_ID}")
    val merOppfolgingFrontendClientId: String,
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val veilarbregistreringClient: VeilarbregistreringClient,
    val isOppfolgingstilfelleClient: IsOppfolgingstilfelleClient,
    val varselService: VarselService,
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

    @PostMapping("/visit")
    @ResponseBody
    fun visit() {
        val innloggetFnr = tokenValidator.validateTokenXClaims().getFnr()
        varselService.ferdigstillMerOppfolgingVarsel(innloggetFnr)
    }

    @PostMapping("/submit")
    @ResponseBody
    fun submit(
        @RequestBody senOppfolgingRegistrering: SenOppfolgingRegistrering,
    ) {
        tokenValidator.validateTokenXClaims()
        val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, TOKENX)
        veilarbregistreringClient.completeRegistration(token, senOppfolgingRegistrering)
        metric.countSenOppfolgingSubmitted()
    }

    @PostMapping("/submitform")
    @ResponseBody
    fun submitForm(
        @RequestBody senOppfolgingDTOV1: SenOppfolgingDTOV1,
    ) {
        val personident = tokenValidator.validateTokenXClaims().getFnr()
        val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, TOKENX)

        responseDao.saveFormResponse(
            PersonIdentNumber(personident),
            senOppfolgingDTOV1.senOppfolgingFormV1.map { it.toQuestionResponse() },
            FormType.SEN_OPPFOLGING_V1,
        )

        if (senOppfolgingDTOV1.senOppfolgingRegistrering != null &&
            senOppfolgingDTOV1.senOppfolgingFormV1.trengerOppfolging()
        ) {
            veilarbregistreringClient.completeRegistration(token, senOppfolgingDTOV1.senOppfolgingRegistrering)
        }

        metric.countSenOppfolgingSubmitted()
    }

    fun SenOppfolgingQuestionV1.toQuestionResponse(): QuestionResponse {
        return QuestionResponse(questionType.name, questionText, answerType, answerText)
    }
}

private fun List<SenOppfolgingQuestionV1>.trengerOppfolging(): Boolean {
    val trengerOppfolgingQuestion = find { it.questionType == SenOppfolgingQuestionTypeV1.ONSKER_OPPFOLGING }
    checkNotNull(trengerOppfolgingQuestion)
    val answer = OnskerOppfolgingSvar.valueOf(trengerOppfolgingQuestion.answerType)
    return answer == OnskerOppfolgingSvar.JA
}
