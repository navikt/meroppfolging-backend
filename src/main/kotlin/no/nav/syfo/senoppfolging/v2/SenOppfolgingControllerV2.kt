package no.nav.syfo.senoppfolging.v2

import jakarta.annotation.PostConstruct
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenUtil
import no.nav.syfo.auth.TokenUtil.TokenIssuer.TOKENX
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.auth.getFnr
import no.nav.syfo.besvarelse.database.ResponseDao
import no.nav.syfo.besvarelse.database.domain.FormType
import no.nav.syfo.dokarkiv.DokarkivClient
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.logger
import no.nav.syfo.maksdato.EsyfovarselClient
import no.nav.syfo.metric.Metric
import no.nav.syfo.senoppfolging.AlreadyRespondedException
import no.nav.syfo.senoppfolging.NoUtsendtVarselException
import no.nav.syfo.senoppfolging.kafka.KSenOppfolgingSvarDTO
import no.nav.syfo.senoppfolging.kafka.SenOppfolgingSvarKafkaProducer
import no.nav.syfo.senoppfolging.v2.domain.ResponseStatus
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingDTOV2
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingStatusDTOV2
import no.nav.syfo.senoppfolging.v2.domain.behovForOppfolging
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
class SenOppfolgingControllerV2(
    @Value("\${MEROPPFOLGING_FRONTEND_CLIENT_ID}")
    val merOppfolgingFrontendClientId: String,
    @Value("\${ESYFO_PROXY_CLIENT_ID}")
    val esyfoProxyClientId: String,
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val varselService: VarselService,
    val metric: Metric,
    val responseDao: ResponseDao,
    val senOppfolgingSvarKafkaProducer: SenOppfolgingSvarKafkaProducer,
    val esyfovarselClient: EsyfovarselClient,
    val dokarkivClient: DokarkivClient,
    val syfoopfpdfgenService: PdfgenService,
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

        val hasUtsendtVarsel = varselService.getUtsendtVarsel(personIdent) != null

        val response =
            responseDao.findLatestFormResponse(
                PersonIdentNumber(personIdent),
                FormType.SEN_OPPFOLGING_V2,
                cutoffDate,
            )
        val sykepengerMaxDateResponse = esyfovarselClient.getSykepengerMaxDateResponse(token)

        return SenOppfolgingStatusDTOV2(
            responseStatus = response?.questionResponses?.toResponseStatus() ?: ResponseStatus.NO_RESPONSE,
            response = response?.questionResponses,
            responseTime = response?.createdAt?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            maxDate = sykepengerMaxDateResponse?.maxDate,
            gjenstaendeSykedager = sykepengerMaxDateResponse?.gjenstaendeSykedager,
            hasAccessToSenOppfolging = hasUtsendtVarsel,
        )
    }

    @PostMapping("/submitform")
    @ResponseBody
    fun submitForm(
        @RequestBody senOppfolgingDTOV2: SenOppfolgingDTOV2,
    ) {
        if (senOppfolgingDTOV2.senOppfolgingFormV2.behovForOppfolging()) {
            metric.countSenOppfolgingRequestYes()
        } else {
            metric.countSenOppfolgingRequestNo()
        }
        val personident = tokenValidator.validateTokenXClaims().getFnr()
        val response =
            responseDao.find(
                personIdent = PersonIdentNumber(personident),
                formType = FormType.SEN_OPPFOLGING_V2,
                from = cutoffDate,
            )

        if (response.isNotEmpty()) {
            throw AlreadyRespondedException()
        }

        val createdAt = LocalDateTime.now()
        val utsendtVarsel =
            varselService.getUtsendtVarsel(personident) ?: throw NoUtsendtVarselException().also {
                log.error("No varsel found. This should not happen.")
            }

        val id =
            responseDao.saveFormResponse(
                personIdent = PersonIdentNumber(personident),
                questionResponses = senOppfolgingDTOV2.senOppfolgingFormV2.map { it.toQuestionResponse() },
                formType = FormType.SEN_OPPFOLGING_V2,
                createdAt = createdAt,
                utsendtVarselUUID = utsendtVarsel.uuid,
            )

        varselService.ferdigstillMerOppfolgingVarsel(personident)

        val pdf = syfoopfpdfgenService.getSenOppfolgingReceiptPdf(senOppfolgingDTOV2.senOppfolgingFormV2)
        if (pdf == null) {
            log.error("Failed to generate PDF")
        } else {
            log.info("Generated PDF")
            dokarkivClient.postDocumentToDokarkiv(fnr = personident, pdf = pdf, uuid = id.toString())
        }

        senOppfolgingSvarKafkaProducer
            .publishResponse(
                KSenOppfolgingSvarDTO(
                    id = id,
                    personIdent = personident,
                    createdAt = createdAt,
                    response = senOppfolgingDTOV2.senOppfolgingFormV2,
                    varselId = utsendtVarsel.uuid,
                ),
            )
        if (senOppfolgingDTOV2.senOppfolgingFormV2.behovForOppfolging()) {
            metric.countSenOppfolgingV2Submitted()
        } else {
            metric.countSenOppfolgingV2SubmittedNo()
        }

        metric.countSenOppfolgingV2Submitted()
    }
}
