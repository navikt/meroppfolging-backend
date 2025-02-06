package no.nav.syfo.besvarelse.database.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.syfo.domain.PersonIdentNumber
import java.time.LocalDateTime
import java.util.UUID

class FormResponseTest : StringSpec({
    "should arrange question responses in fixed order" {
        val questionResponse2 = QuestionResponse(
            "BEHOV_FOR_OPPFOLGING",
            "Ønsker du å be om oppfølging?",
            "JA",
            "Ja, jeg ønsker å be om oppfølging"
        )

        val questionResponse1 = QuestionResponse(
            "FREMTIDIG_SITUASJON",
            "Hvilken situasjon tror du at du er i når sykepengene har tatt slutt?",
            "FORTSATT_SYK",
            "Jeg er for syk til å jobbe"
        )

        val formResponse = FormResponse(
            uuid = UUID.randomUUID(),
            personIdent = PersonIdentNumber("12345678911"),
            createdAt = LocalDateTime.now(),
            formType = FormType.SEN_OPPFOLGING_V2,
            questionResponses = mutableListOf(questionResponse2, questionResponse1)
        )

        val arrangedFormResponse = formResponse.arrangeQuestionResponsesInFixedOrder()

        arrangedFormResponse.questionResponses shouldBe listOf(questionResponse1, questionResponse2)
    }
})
