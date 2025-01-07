package no.nav.syfo.senoppfolging.service

import SingleDocumentData
import no.nav.syfo.besvarelse.database.ResponseDao
import no.nav.syfo.besvarelse.database.domain.FormResponse
import no.nav.syfo.besvarelse.database.domain.FormType
import no.nav.syfo.dokarkiv.DokarkivClient
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.logger
import no.nav.syfo.metric.Metric
import no.nav.syfo.oppfolgingstilfelle.IsOppfolgingstilfelleClient
import no.nav.syfo.oppfolgingstilfelle.Oppfolgingstilfelle
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
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class SenOppfolgingService(
    val sykepengedagerInformasjonService: SykepengedagerInformasjonService,
    val isOppfolgingstilfelleClient: IsOppfolgingstilfelleClient,
    val varselService: VarselService,
    val responseDao: ResponseDao,
    val metric: Metric,
    val senOppfolgingSvarKafkaProducer: SenOppfolgingSvarKafkaProducer,
    val dokarkivClient: DokarkivClient,
    val syfoopfpdfgenService: PdfgenService,
) {
    private val log = logger()

    fun prepareAndBuildSenOppfolgingStatusDTOV2(
        personIdent: String,
        token: String,
    ): SenOppfolgingStatusDTOV2 {
        val sykepengerInformasjon =
            sykepengedagerInformasjonService.fetchSykepengedagerInformasjonByIdent(personIdent)

        val userAccess = getUserAccess(personIdent, token)
        val hasAccess = userAccess is UserAccess.Good
        val varsel = (userAccess as? UserAccess.Good)?.varsel
        val response = getResponseOrNull(varsel, personIdent)

        return SenOppfolgingStatusDTOV2(
            responseStatus = response?.questionResponses?.toResponseStatus() ?: ResponseStatus.NO_RESPONSE,
            response = response?.questionResponses,
            responseTime = response?.createdAt?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            maxDate = sykepengerInformasjon?.forelopigBeregnetSluttFormatted(),
            gjenstaendeSykedager = sykepengerInformasjon?.gjenstaendeSykedager,
            hasAccessToSenOppfolging = hasAccess,
        )
    }

    fun handleSubmitForm(
        personIdent: String,
        senOppfolgingForm: SenOppfolgingDTOV2,
        varsel: Varsel,
    ) {
        countMetricsForSvarBeforeProcessing(senOppfolgingForm)

        val currentDateTime = LocalDateTime.now()
        val currentDate = currentDateTime.toLocalDate()

        val formResponseId =
            responseDao.saveFormResponse(
                personIdent = PersonIdentNumber(personIdent),
                questionResponses = senOppfolgingForm.senOppfolgingFormV2.map { it.toQuestionResponse() },
                formType = FormType.SEN_OPPFOLGING_V2,
                createdAt = currentDateTime,
                utsendtVarselUUID = varsel.uuid,
            )

        varselService.ferdigstillMerOppfolgingVarsel(personIdent)

        generateAndSendFormStepsAndReceiptPdfsToDokarkiv(formResponseId, personIdent, senOppfolgingForm, currentDate)

        publishSenOppfolgingSvarToKafka(formResponseId, personIdent, currentDateTime, senOppfolgingForm, varsel)

        countMetricsForSvarAfterProcessing(senOppfolgingForm)
    }

    fun getUserAccess(
        personIdent: String,
        token: String,
    ): UserAccess {
        val oppfolgingstilfelle = isOppfolgingstilfelleClient.getOppfolgingstilfeller(token)
        val varsel = varselService.getUtsendtVarsel(personIdent)

        return createUserAccess(varsel, oppfolgingstilfelle)
    }

    fun getResponseOrNull(
        varsel: Varsel?,
        personIdent: String,
    ): FormResponse? =
        // run block is necessary as long as users with utsendt varsel has response without varsel id
        varsel?.let {
            responseDao.findResponseByVarselId(it.uuid)
                ?: responseDao.findLatestFormResponse(
                    PersonIdentNumber(personIdent),
                    FormType.SEN_OPPFOLGING_V2,
                    varsel.utsendtTidspunkt.toLocalDate(),
                )
        }

    private fun generateAndSendFormStepsAndReceiptPdfsToDokarkiv(
        storedSubmissionId: UUID,
        personident: String,
        formResponse: SenOppfolgingDTOV2,
        submissionDate: LocalDate,
    ) {
        val documentsData = mutableListOf<SingleDocumentData>()

        val formStepsPdf = syfoopfpdfgenService.getSenOppfolgingFormStepsPdf(
            formResponse.senOppfolgingFormV2,
        )

        val receiptPdf = syfoopfpdfgenService.getSenOppfolgingReceiptPdf(
            personident,
            formResponse.senOppfolgingFormV2,
            submissionDate
        )

        if (formStepsPdf != null) {
            documentsData.add(
                SingleDocumentData(
                    pdf = formStepsPdf,
                    filnavn = "SSPS-skjema-utfylling",
                    title = "Snart slutt på sykepenger - Din utfylling av skjema"
                )
            )
        }

        if (receiptPdf != null) {
            documentsData.add(
                SingleDocumentData(
                    pdf = receiptPdf,
                    filnavn = "SSPS-kvittering",
                    title = "Snart slutt på sykepenger - Kvittering på ditt svar",
                )
            )
        }

        if (documentsData.size == 0) {
            log.error("Neither formSteps or receipt PDF was generated, skipping sending forsendelse to dokarkiv")
        } else {
            if (documentsData.size == 1) {
                log.error(
                    "Only one of formSteps or receipt PDF generated (${documentsData.first().filnavn}), " +
                        "sending that one to dokarkiv"
                )
            }

            log.info("Sending forsendelse to dokarkiv for formSteps and receipt PDFs")

            dokarkivClient.postDocumentsForsendelseToDokarkiv(
                fnr = personident,
                documentsData = documentsData,
                forsendelseTittel = "SSPS - Utfylling av skjema og kvittering",
                eksternReferanseId = storedSubmissionId.toString(),
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

enum class UserAccessError {
    NoUtsendtVarsel,
    NoAccessToSenOppfolging,
}

sealed class UserAccess {
    data class Good(
        val varsel: Varsel,
    ) : UserAccess()

    data class Error(
        val error: UserAccessError,
    ) : UserAccess()
}

private fun createUserAccess(
    varsel: Varsel?,
    oppfolgingstilfelle: List<Oppfolgingstilfelle>,
): UserAccess {
    if (!oppfolgingstilfelle.isInOppfolgingstilfellePlus16Days()) {
        return UserAccess.Error(UserAccessError.NoAccessToSenOppfolging)
    }
    if (varsel == null) {
        return UserAccess.Error(UserAccessError.NoUtsendtVarsel)
    }
    return UserAccess.Good(varsel)
}
