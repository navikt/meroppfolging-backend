package no.nav.syfo.syfoopppdfgen

import no.nav.syfo.dkif.DkifClient
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionTypeV2
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionV2
import no.nav.syfo.senoppfolging.v2.domain.behovForOppfolging
import no.nav.syfo.sykepengedagerinformasjon.domain.forelopigBeregnetSluttFormatted
import no.nav.syfo.sykepengedagerinformasjon.domain.forelopigBeregnetSluttISO
import no.nav.syfo.sykepengedagerinformasjon.domain.utbetaltTomFormatted
import no.nav.syfo.sykepengedagerinformasjon.domain.utbetaltTomISO
import no.nav.syfo.sykepengedagerinformasjon.service.SykepengedagerInformasjonService
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class PdfgenService(
    val syfooppfpdfgenClient: PdfgenClient,
    val dkifClient: DkifClient,
    val sykepengedagerInformasjonService: SykepengedagerInformasjonService,
) {
    fun getSenOppfolgingReceiptPdf(
        personIdent: String,
        answersToQuestions: List<SenOppfolgingQuestionV2>,
        submissionDate: LocalDate,
    ): ByteArray? {
        val behovForOppfolging = answersToQuestions.behovForOppfolging()
        val sykepengerInformasjon = sykepengedagerInformasjonService.fetchSykepengedagerInformasjonByIdent(
            personIdent,
        )

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
            submissionDateISO = submissionDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            maxDateISO = sykepengerInformasjon?.forelopigBeregnetSluttISO(),
            utbetaltTomISO = sykepengerInformasjon?.utbetaltTomISO(),
            daysUntilMaxDate = sykepengerInformasjon?.gjenstaendeSykedager.toString(),
        )
    }

    fun getSenOppfolgingLandingPdf(personIdent: String): ByteArray {
        val isUserReservert = dkifClient.person(personIdent).kanVarsles == false
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
}
