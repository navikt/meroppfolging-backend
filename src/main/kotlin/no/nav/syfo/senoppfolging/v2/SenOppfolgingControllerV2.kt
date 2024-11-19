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
import no.nav.syfo.metric.Metric
import no.nav.syfo.oppfolgingstilfelle.IsOppfolgingstilfelleClient
import no.nav.syfo.oppfolgingstilfelle.Oppfolgingstilfelle
import no.nav.syfo.senoppfolging.AlreadyRespondedException
import no.nav.syfo.senoppfolging.NoAccessToSenOppfolgingException
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
import no.nav.syfo.sykepengedagerinformasjon.domain.forelopigBeregnetSluttFormatted
import no.nav.syfo.sykepengedagerinformasjon.service.SykepengedagerInformasjonService
import no.nav.syfo.varsel.Varsel
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
import java.util.UUID

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
    val sykepengedagerInformasjonService: SykepengedagerInformasjonService,
    val senOppfolgingSvarKafkaProducer: SenOppfolgingSvarKafkaProducer,
    val dokarkivClient: DokarkivClient,
    val syfoopfpdfgenService: PdfgenService,
    val isOppfolgingstilfelleClient: IsOppfolgingstilfelleClient,
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
        val personIdent = tokenValidator.validateTokenXClaims().getFnr()
        val response =
            responseDao.findLatestFormResponse(
                PersonIdentNumber(personIdent),
                FormType.SEN_OPPFOLGING_V2,
                cutoffDate,
            )
        val sykepengerInformasjon = sykepengedagerInformasjonService.fetchSykepengedagerInformasjonByIdent(
            personIdent
        )

        val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, TOKENX)

        val oppfolgingstilfelle = isOppfolgingstilfelleClient.getOppfolgingstilfeller(token)
        val varsel = varselService.getUtsendtVarsel(personIdent)
        val hasAccess =
            when (createUserAccess(varsel, oppfolgingstilfelle)) {
                is UserAccess.Good -> true
                is UserAccess.Error -> false
            }

        return SenOppfolgingStatusDTOV2(
            responseStatus = response?.questionResponses?.toResponseStatus() ?: ResponseStatus.NO_RESPONSE,
            response = response?.questionResponses,
            responseTime = response?.createdAt?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            maxDate = sykepengerInformasjon?.forelopigBeregnetSluttFormatted(),
            gjenstaendeSykedager = sykepengerInformasjon?.gjenstaendeSykedager,
            hasAccessToSenOppfolging = hasAccess,
        )
    }

    @PostMapping("/submitform")
    @ResponseBody
    fun submitForm(
        @RequestBody senOppfolgingSvar: SenOppfolgingDTOV2,
    ) {
        countMetricsForSvarBeforeProcessing(senOppfolgingSvar)

        val personident = tokenValidator.validateTokenXClaims().getFnr()
        val response =
            responseDao.find(
                personIdent = PersonIdentNumber(personident),
                formType = FormType.SEN_OPPFOLGING_V2,
                from = cutoffDate,
            )

        if (response.isNotEmpty()) {
            throw AlreadyRespondedException().also {
                log.error(
                    "User has already responded in the last 3 months.",
                )
            }
        }

        val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, TOKENX)
        val oppfolgingstilfelle = isOppfolgingstilfelleClient.getOppfolgingstilfeller(token)
        val varsel = varselService.getUtsendtVarsel(personident)
        val utsendtVarsel = validateVarselAndAccess(varsel, oppfolgingstilfelle)

        val createdAt = LocalDateTime.now()
        val id =
            responseDao.saveFormResponse(
                personIdent = PersonIdentNumber(personident),
                questionResponses = senOppfolgingSvar.senOppfolgingFormV2.map { it.toQuestionResponse() },
                formType = FormType.SEN_OPPFOLGING_V2,
                createdAt = createdAt,
                utsendtVarselUUID = utsendtVarsel.uuid,
            )

        varselService.ferdigstillMerOppfolgingVarsel(personident)

        generateAndSendPDFToDokarkiv(personident, senOppfolgingSvar, id)

        publishSenOppfolgingSvarToKafka(id, personident, createdAt, senOppfolgingSvar, utsendtVarsel)

        countMetricsForSvarAfterProcessing(senOppfolgingSvar)
    }

    private fun validateVarselAndAccess(
        varsel: Varsel?,
        oppfolgingstilfelle: List<Oppfolgingstilfelle>,
    ): Varsel {
        val userData = createUserAccess(varsel, oppfolgingstilfelle)

        return when (userData) {
            is UserAccess.Good -> userData.varsel
            is UserAccess.Error -> {
                when (userData.error) {
                    UserDataError.NoUtsendtVarsel -> throw NoUtsendtVarselException().also {
                        log.error("User has no valid varsel.")
                    }
                    UserDataError.NoAccessToSenOppfolging -> throw NoAccessToSenOppfolgingException().also {
                        log.error("User is not in a oppfolgingtilfelle + 16 days.")
                    }
                }
            }
        }
    }

    private fun generateAndSendPDFToDokarkiv(
        personident: String,
        formResponse: SenOppfolgingDTOV2,
        id: UUID,
    ) {
        val pdf = syfoopfpdfgenService.getSenOppfolgingReceiptPdf(personident, formResponse.senOppfolgingFormV2)
        if (pdf == null) {
            log.error("Failed to generate PDF")
        } else {
            log.info("Generated PDF")
            dokarkivClient.postDocumentToDokarkiv(
                fnr = personident,
                pdf = pdf,
                uuid = id.toString(),
                title = "Snart slutt på sykepenger - Kvittering på ditt svar",
                filnavnBeforeUUID = "SSPS-kvittering",
            )
        }
    }

    private fun publishSenOppfolgingSvarToKafka(
        id: UUID,
        personident: String,
        createdAt: LocalDateTime,
        formResponse: SenOppfolgingDTOV2,
        utsendtVarsel: Varsel,
    ) {
        senOppfolgingSvarKafkaProducer
            .publishResponse(
                KSenOppfolgingSvarDTO(
                    id = id,
                    personIdent = personident,
                    createdAt = createdAt,
                    response = formResponse.senOppfolgingFormV2,
                    varselId = utsendtVarsel.uuid,
                ),
            )
    }

    private fun countMetricsForSvarBeforeProcessing(formResponse: SenOppfolgingDTOV2) {
        if (formResponse.senOppfolgingFormV2.behovForOppfolging()) {
            metric.countSenOppfolgingRequestYes()
        } else {
            metric.countSenOppfolgingRequestNo()
        }
    }

    private fun countMetricsForSvarAfterProcessing(formResponse: SenOppfolgingDTOV2) {
        if (formResponse.senOppfolgingFormV2.behovForOppfolging()) {
            metric.countSenOppfolgingV2SubmittedYes()
        } else {
            metric.countSenOppfolgingV2SubmittedNo()
        }
    }
}

private fun List<Oppfolgingstilfelle>.isInOppfolgingstilfellePlus16Days() =
    this.firstOrNull()?.let {
        LocalDate.now().isBefore(it.end.plusDays(17))
    } ?: false

private enum class UserDataError {
    NoUtsendtVarsel,
    NoAccessToSenOppfolging,
}

private sealed class UserAccess {
    data class Good(
        val varsel: Varsel,
    ) : UserAccess()

    data class Error(
        val error: UserDataError,
    ) : UserAccess()
}

private fun createUserAccess(
    varsel: Varsel?,
    oppfolgingstilfelle: List<Oppfolgingstilfelle>,
): UserAccess {
    if (!oppfolgingstilfelle.isInOppfolgingstilfellePlus16Days()) {
        return UserAccess.Error(UserDataError.NoAccessToSenOppfolging)
    }
    if (varsel == null) {
        return UserAccess.Error(UserDataError.NoUtsendtVarsel)
    }
    return UserAccess.Good(varsel)
}
