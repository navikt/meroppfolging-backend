package no.nav.syfo.syfoopppdfgen

import no.nav.syfo.kartlegging.domain.Kartleggingssporsmal
import no.nav.syfo.kartlegging.domain.formsnapshot.CheckboxFieldSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.FieldSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.RadioGroupFieldSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.SingleCheckboxFieldSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.TextFieldSnapshot
import no.nav.syfo.logger
import no.nav.syfo.senoppfolging.v2.domain.BehovForOppfolgingSvar
import no.nav.syfo.senoppfolging.v2.domain.FremtidigSituasjonSvar
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionTypeV2
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionV2
import no.nav.syfo.senoppfolging.v2.domain.behovForOppfolging
import no.nav.syfo.sykepengedagerinformasjon.domain.forelopigBeregnetSluttFormatted
import no.nav.syfo.sykepengedagerinformasjon.domain.utbetaltTomFormatted
import no.nav.syfo.sykepengedagerinformasjon.service.SykepengedagerInformasjonService
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class PdfgenService(
    val syfooppfpdfgenClient: PdfgenClient,
    val sykepengedagerInformasjonService: SykepengedagerInformasjonService,
) {
    private val log = logger()

    fun getSenOppfolgingLandingPdf(personIdent: String, isUserReservert: Boolean): ByteArray {
        val sykepengerInformasjon = sykepengedagerInformasjonService.fetchSykepengedagerInformasjonByIdent(
            personIdent,
        )

        return syfooppfpdfgenClient.createSenOppfolgingLandingPdf(
            daysLeft = sykepengerInformasjon?.gjenstaendeSykedager.toString(),
            utbetaltTom = sykepengerInformasjon?.utbetaltTomFormatted(),
            maxDateFormatted = sykepengerInformasjon?.forelopigBeregnetSluttFormatted(),
            isForReservertUser = isUserReservert,
        )
    }

    fun getSenOppfolgingFormStepsPdf(
        answersToQuestions: List<SenOppfolgingQuestionV2>,
    ): ByteArray? {
        val fremtidigSituasjonSvar: FremtidigSituasjonSvar? = answersToQuestions.firstOrNull {
            it.questionType == SenOppfolgingQuestionTypeV2.FREMTIDIG_SITUASJON
        }?.let {
            FremtidigSituasjonSvar.valueOf(it.answerType)
        }

        val behovForOppfolgingSvar: BehovForOppfolgingSvar? = answersToQuestions.firstOrNull {
            it.questionType == SenOppfolgingQuestionTypeV2.BEHOV_FOR_OPPFOLGING
        }?.let {
            BehovForOppfolgingSvar.valueOf(it.answerType)
        }

        try {
            return syfooppfpdfgenClient.createSenOppfolgingFormStepsPdf(
                fremtidigSituasjonSvar,
                behovForOppfolgingSvar,
            )
        } catch (e: Exception) {
            log.error(
                "Failed to create senOppfolging formSteps PDF, message: ${e.message}, cause: ${e.cause}"
            )

            return null
        }
    }

    fun getSenOppfolgingReceiptPdf(
        personIdent: String,
        answersToQuestions: List<SenOppfolgingQuestionV2>,
        submissionDate: LocalDate,
    ): ByteArray? {
        val behovForOppfolging = answersToQuestions.behovForOppfolging()
        val sykepengerInformasjon = sykepengedagerInformasjonService.fetchSykepengedagerInformasjonByIdent(
            personIdent,
        )

        try {
            return syfooppfpdfgenClient.createSenOppfolgingReceiptPdf(
                behovForOppfolging = behovForOppfolging,
                questionTextFremtidigSituasjon = answersToQuestions.firstOrNull
                    { it.questionType == SenOppfolgingQuestionTypeV2.FREMTIDIG_SITUASJON }?.questionText,
                answerTextFremtidigSituasjon = answersToQuestions.firstOrNull {
                    it.questionType == SenOppfolgingQuestionTypeV2.FREMTIDIG_SITUASJON
                }?.answerText,
                questionTextBehovForOppfolging = answersToQuestions.firstOrNull {
                    it.questionType == SenOppfolgingQuestionTypeV2.BEHOV_FOR_OPPFOLGING
                }?.questionText,
                answerTextBehovForOppfolging = answersToQuestions.firstOrNull {
                    it.questionType == SenOppfolgingQuestionTypeV2.BEHOV_FOR_OPPFOLGING
                }?.answerText,
                submissionDateFormatted = submissionDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                maxDateFormatted = sykepengerInformasjon?.forelopigBeregnetSluttFormatted(),
                utbetaltTomFormatted = sykepengerInformasjon?.utbetaltTomFormatted(),
                daysUntilMaxDate = sykepengerInformasjon?.gjenstaendeSykedager.toString(),
            )
        } catch (e: Exception) {
            log.error(
                "Failed to create senOppfolging receipt PDF, message: ${e.message}, cause: ${e.cause}"
            )

            return null
        }
    }

    fun getKartleggingsPdf(kartleggingssporsmal: Kartleggingssporsmal, createdAt: Instant): ByteArray? {
        val request = kartleggingssporsmal.toKartleggingPdfgenRequest(createdAt)
        return syfooppfpdfgenClient.getKartleggingPdf(request)
    }

    fun Kartleggingssporsmal.toKartleggingPdfgenRequest(createdAt: Instant): KartleggingPdfgenRequest {
        val inputFields = this.formSnapshot.fieldSnapshots.map { it.toInputField() }
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        return KartleggingPdfgenRequest(
            inputFields = inputFields,
            createdAt = createdAt
                .atZone(ZoneId.of("Europe/Oslo"))
                .toLocalDate()
                .format(formatter),
        )
    }

    fun FieldSnapshot.toInputField(): PdfInputField {
        return PdfInputField(
            title = this.label,
            value = when (this) {
                is TextFieldSnapshot -> this.value
                is RadioGroupFieldSnapshot -> this.options.first { it.wasSelected }.optionLabel
                is SingleCheckboxFieldSnapshot -> if (this.value) "Ja" else "Nei"
                is CheckboxFieldSnapshot -> this.options
                    .filter { it.wasSelected }
                    .joinToString("\n") { it.optionLabel }
                else -> throw IllegalArgumentException("Unknown field type: ${this.fieldType}")
            },
        )
    }
}
