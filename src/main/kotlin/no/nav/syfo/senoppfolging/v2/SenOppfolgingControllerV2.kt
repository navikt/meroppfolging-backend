package no.nav.syfo.senoppfolging.v2

import jakarta.annotation.PostConstruct
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.auth.getFnr
import no.nav.syfo.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.behandlendeenhet.domain.isPilot
import no.nav.syfo.besvarelse.database.ResponseDao
import no.nav.syfo.besvarelse.database.domain.FormType
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.logger
import no.nav.syfo.metric.Metric
import no.nav.syfo.senoppfolging.AlreadyRespondedException
import no.nav.syfo.senoppfolging.kafka.KSenOppfolgingSvarDTOV2
import no.nav.syfo.senoppfolging.kafka.SenOppfolgingSvarKafkaProducer
import no.nav.syfo.senoppfolging.v2.domain.ResponseStatus
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingDTOV2
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingStatusDTOV2
import no.nav.syfo.senoppfolging.v2.domain.toQuestionResponse
import no.nav.syfo.senoppfolging.v2.domain.toResponseStatus
import no.nav.syfo.varsel.VarselService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v2/senoppfolging")
@ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
@Suppress("LongParameterList")
class SenOppfolgingControllerV2(
    @Value("\${MEROPPFOLGING_FRONTEND_CLIENT_ID}")
    val merOppfolgingFrontendClientId: String,
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val varselService: VarselService,
    val metric: Metric,
    val responseDao: ResponseDao,
    val behandlendeEnhetClient: BehandlendeEnhetClient,
    val senOppfolgingSvarKafkaProducer: SenOppfolgingSvarKafkaProducer,
    @Value("\${toggle.pilot}") private var pilotEnabledForEnvironment: Boolean,
) {
    lateinit var tokenValidator: TokenValidator
    private val log = logger()
    private val cutoffDate = LocalDate.now().minusMonths(3)

    @PostConstruct
    fun init() {
        tokenValidator = TokenValidator(tokenValidationContextHolder, merOppfolgingFrontendClientId)
    }

    @GetMapping("/status", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun status(): SenOppfolgingStatusDTOV2 {
        if (!pilotEnabledForEnvironment) {
            return SenOppfolgingStatusDTOV2(
                isPilot = false,
                responseStatus = ResponseStatus.NO_RESPONSE,
            )
        }

        val personIdent = tokenValidator.validateTokenXClaims().getFnr()
        val behandlendeEnhet = behandlendeEnhetClient.getBehandlendeEnhet(personIdent)
        log.info("Behandlende enhet: ${behandlendeEnhet.enhetId}")
        val response = responseDao.find(
            PersonIdentNumber(personIdent),
            FormType.SEN_OPPFOLGING_V2,
            cutoffDate,
        )
        return SenOppfolgingStatusDTOV2(
            isPilot = behandlendeEnhet.isPilot(),
            responseStatus = response.toResponseStatus(),
        )
    }

    @PostMapping("/submitform")
    @ResponseBody
    fun submitForm(
        @RequestBody senOppfolgingDTOV2: SenOppfolgingDTOV2,
    ) {
        if (!pilotEnabledForEnvironment) {
            return
        }

        val personident = tokenValidator.validateTokenXClaims().getFnr()
        val response = responseDao.find(
            personIdent = PersonIdentNumber(personident),
            formType = FormType.SEN_OPPFOLGING_V2,
            from = cutoffDate,
        )

        varselService.ferdigstillMerOppfolgingVarsel(personident)

        if (response.isNotEmpty()) {
            throw AlreadyRespondedException()
        }

        val createdAt = LocalDateTime.now()
        val id = responseDao.saveFormResponse(
            personIdent = PersonIdentNumber(personident),
            questionResponses = senOppfolgingDTOV2.senOppfolgingFormV2.map { it.toQuestionResponse() },
            formType = FormType.SEN_OPPFOLGING_V2,
            createdAt = createdAt,
        )

        senOppfolgingSvarKafkaProducer
            .publishResponse(
                KSenOppfolgingSvarDTOV2(id, personident, createdAt, senOppfolgingDTOV2.senOppfolgingFormV2)
            )

        metric.countSenOppfolgingSubmitted()
    }
}
