package no.nav.syfo.senoppfolging.v2

import jakarta.annotation.PostConstruct
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenUtil
import no.nav.syfo.auth.TokenUtil.TokenIssuer.TOKENX
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.auth.getFnr
import no.nav.syfo.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.behandlendeenhet.domain.isPilot
import no.nav.syfo.besvarelse.database.ResponseDao
import no.nav.syfo.besvarelse.database.domain.FormType
import no.nav.syfo.dokarkiv.DokarkivClient
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.logger
import no.nav.syfo.maksdato.EsyfovarselClient
import no.nav.syfo.metric.Metric
import no.nav.syfo.senoppfolging.AlreadyRespondedException
import no.nav.syfo.senoppfolging.kafka.KSenOppfolgingSvarDTO
import no.nav.syfo.senoppfolging.kafka.SenOppfolgingSvarKafkaProducer
import no.nav.syfo.senoppfolging.v2.domain.ResponseStatus
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingDTOV2
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingStatusDTOV2
import no.nav.syfo.senoppfolging.v2.domain.toQuestionResponse
import no.nav.syfo.senoppfolging.v2.domain.toResponseStatus
import no.nav.syfo.syfoopppdfgen.PdfgenService
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
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/v2/senoppfolging")
@ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
@Suppress("LongParameterList")
class SenOppfolgingControllerV2(
    @Value("\${MEROPPFOLGING_FRONTEND_CLIENT_ID}")
    val merOppfolgingFrontendClientId: String,
    @Value("\${ESYFO_PROXY_CLIENT_ID}")
    val esyfoProxyClientId: String,
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val varselService: VarselService,
    val metric: Metric,
    val responseDao: ResponseDao,
    val behandlendeEnhetClient: BehandlendeEnhetClient,
    val senOppfolgingSvarKafkaProducer: SenOppfolgingSvarKafkaProducer,
    val esyfovarselClient: EsyfovarselClient,
    val dokarkivClient: DokarkivClient,
    @Value("\${toggle.pilot}") private var pilotEnabledForEnvironment: Boolean,
    val syfoopfpdfgenService: PdfgenService,
    @Value("\${NAIS_CLUSTER_NAME}") private var clusterName: String,
) {
    lateinit var tokenValidator: TokenValidator
    private val log = logger()
    private val cutoffDate = LocalDate.now().minusMonths(3)

    @PostConstruct
    fun init() {
        tokenValidator =
            TokenValidator(tokenValidationContextHolder, listOf(merOppfolgingFrontendClientId, esyfoProxyClientId))
    }

    @GetMapping("/status", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun status(): SenOppfolgingStatusDTOV2 {
        val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, TOKENX)
        val personIdent = tokenValidator.validateTokenXClaims().getFnr()
        val behandlendeEnhet = behandlendeEnhetClient.getBehandlendeEnhet(personIdent)
        val isProd = "prod-gcp" == clusterName
        log.info("Behandlende enhet: ${behandlendeEnhet.enhetId}")

        if (!pilotEnabledForEnvironment || hasRespondedToV1Form(personIdent)) {
            return SenOppfolgingStatusDTOV2(
                isPilot = false,
                responseStatus = ResponseStatus.NO_RESPONSE,
                response = null,
                responseTime = null,
                maxDate = null,
                gjenstaendeSykedager = null,
            )
        }

        val response =
            responseDao.findLatestFormResponse(
                PersonIdentNumber(personIdent),
                FormType.SEN_OPPFOLGING_V2,
                cutoffDate,
            )
        val sykepengerMaxDateResponse = esyfovarselClient.getSykepengerMaxDateResponse(token)

        return SenOppfolgingStatusDTOV2(
            isPilot = behandlendeEnhet.isPilot(isProd = isProd),
            responseStatus = response?.questionResponses?.toResponseStatus() ?: ResponseStatus.NO_RESPONSE,
            response = response?.questionResponses,
            responseTime = response?.createdAt?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            maxDate = sykepengerMaxDateResponse?.maxDate,
            gjenstaendeSykedager = sykepengerMaxDateResponse?.gjenstaendeSykedager,
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
        val response =
            responseDao.find(
                personIdent = PersonIdentNumber(personident),
                formType = FormType.SEN_OPPFOLGING_V2,
                from = cutoffDate,
            )

        varselService.ferdigstillMerOppfolgingVarsel(personident)

        if (response.isNotEmpty()) {
            throw AlreadyRespondedException()
        }

        val createdAt = LocalDateTime.now()
        val latestVarsel = varselService.getUtsendtVarsel(personident)
        val id =
            responseDao.saveFormResponse(
                personIdent = PersonIdentNumber(personident),
                questionResponses = senOppfolgingDTOV2.senOppfolgingFormV2.map { it.toQuestionResponse() },
                formType = FormType.SEN_OPPFOLGING_V2,
                createdAt = createdAt,
                utsendtVarselUUID = latestVarsel?.uuid,
            )

        val pdf = syfoopfpdfgenService.getPdf(senOppfolgingDTOV2.senOppfolgingFormV2)
        if (pdf == null) {
            log.error("Failed to generate PDF")
        } else {
            log.info("Generated PDF")
            dokarkivClient.postDocumentToDokarkiv(fnr = personident, pdf = pdf, uuid = id.toString())
        }

        senOppfolgingSvarKafkaProducer
            .publishResponse(
                KSenOppfolgingSvarDTO(id, personident, createdAt, senOppfolgingDTOV2.senOppfolgingFormV2),
            )

        metric.countSenOppfolgingPilotSubmitted()
    }

    private fun hasRespondedToV1Form(personIdent: String): Boolean {
        val responseOnV1Form =
            responseDao.find(
                PersonIdentNumber(personIdent),
                FormType.SEN_OPPFOLGING_V1,
                cutoffDate,
            )

        return responseOnV1Form.isNotEmpty()
    }
}
