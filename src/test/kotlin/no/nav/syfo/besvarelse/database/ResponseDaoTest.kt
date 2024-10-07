package no.nav.syfo.besvarelse.database

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContain
import no.nav.syfo.LocalApplication
import no.nav.syfo.besvarelse.database.domain.FormType
import no.nav.syfo.besvarelse.database.domain.QuestionResponse
import no.nav.syfo.domain.PersonIdentNumber
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import java.time.LocalDate
import java.time.LocalDateTime

@EmbeddedKafka
@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
class ResponseDaoTest : DescribeSpec() {
    @Autowired
    private lateinit var responseDao: ResponseDao

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    init {
        extension(SpringExtension)

        beforeTest {
            jdbcTemplate.execute("TRUNCATE TABLE FORM_RESPONSE CASCADE")
            jdbcTemplate.execute("TRUNCATE TABLE QUESTION_RESPONSE CASCADE")
        }

        it("Create besvarelse") {
            val personIdent = PersonIdentNumber("12345678910")
            responseDao.saveFormResponse(
                personIdent,
                listOf(QuestionResponse("UTDANNING", "test", "SVAR_ID", "test")),
                FormType.SEN_OPPFOLGING_V1,
                LocalDateTime.now(),
                null,
            )

            val questionResponses = responseDao.find(
                personIdent,
                FormType.SEN_OPPFOLGING_V1,
                LocalDate.now().minusDays(1),
            )
            assert(questionResponses.size == 1)
        }

        it("Find empty") {
            val personIdent = PersonIdentNumber("12345678911")

            val questionResponses = responseDao.find(
                personIdent,
                FormType.SEN_OPPFOLGING_V1,
                LocalDate.now().minusDays(1),
            )
            assert(questionResponses.isEmpty())
        }

        it("Find empty response") {
            val personIdent = PersonIdentNumber("12345678911")

            val formResponse = responseDao.findLatestFormResponse(
                personIdent,
                FormType.SEN_OPPFOLGING_V1,
                LocalDate.now().minusDays(1),
            )
            assertNull(formResponse)
        }

        it("Find latest response created before from date") {
            val personIdent = PersonIdentNumber("12345678911")
            val firstQuestionResponse = QuestionResponse(
                "FREMTIDIG_SITUASJON",
                "Hvordan ser du for deg din situasjon når sykepengene er slutt?",
                "TILBAKE_HOS_ARBEIDSGIVER",
                "Jeg skal tilbake til min arbeidsgiver",
            )
            responseDao.saveFormResponse(
                personIdent = personIdent,
                questionResponses = listOf(firstQuestionResponse),
                formType = FormType.SEN_OPPFOLGING_V2,
                createdAt = LocalDateTime.now().minusDays(2),
                utsendtVarselUUID = null,
            )

            val formResponse = responseDao.findLatestFormResponse(
                personIdent = personIdent,
                formType = FormType.SEN_OPPFOLGING_V2,
                from = LocalDate.now().minusDays(1),
            )

            assertNull(formResponse)
        }

        it("Find latest response (no from date)") {
            val personIdent = PersonIdentNumber("12345678911")
            val firstQuestionResponse = QuestionResponse(
                "FREMTIDIG_SITUASJON",
                "Hvordan ser du for deg din situasjon når sykepengene er slutt?",
                "TILBAKE_HOS_ARBEIDSGIVER",
                "Jeg skal tilbake til min arbeidsgiver",
            )
            responseDao.saveFormResponse(
                personIdent = personIdent,
                questionResponses = listOf(firstQuestionResponse),
                formType = FormType.SEN_OPPFOLGING_V2,
                createdAt = LocalDateTime.now().minusDays(2),
                utsendtVarselUUID = null,
            )

            val formResponse = responseDao.findLatestFormResponse(
                personIdent = personIdent,
                formType = FormType.SEN_OPPFOLGING_V2,
            )
            checkNotNull(formResponse)
        }

        it("Find latest response") {
            val personIdent = PersonIdentNumber("12345678911")
            val firstQuestionResponse = QuestionResponse(
                "FREMTIDIG_SITUASJON",
                "Hvordan ser du for deg din situasjon når sykepengene er slutt?",
                "TILBAKE_HOS_ARBEIDSGIVER",
                "Jeg skal tilbake til min arbeidsgiver",
            )
            val latestQuestionResponse = QuestionResponse(
                "BEHOV_FOR_OPPFOLGING",
                "Trenger du hjelp fra NAV?",
                "JA",
                "Ja",
            )

            responseDao.saveFormResponse(
                personIdent,
                listOf(firstQuestionResponse),
                FormType.SEN_OPPFOLGING_V2,
                LocalDateTime.now().minusDays(1),
                null,
            )

            responseDao.saveFormResponse(
                personIdent,
                listOf(latestQuestionResponse),
                FormType.SEN_OPPFOLGING_V2,
                LocalDateTime.now(),
                null,
            )

            val formResponse = responseDao.findLatestFormResponse(
                personIdent,
                FormType.SEN_OPPFOLGING_V2,
                LocalDate.now().minusDays(2),
            )

            checkNotNull(formResponse)
            checkNotNull(formResponse.questionResponses)

            val question = formResponse.questionResponses[0]

            assertEquals(latestQuestionResponse.questionType, question.questionType)
            assertEquals(latestQuestionResponse.questionText, question.questionText)
            assertEquals(latestQuestionResponse.answerType, question.answerType)
            assertEquals(latestQuestionResponse.answerText, question.answerText)
        }

        it("Find latest response with multiple questions") {
            val personIdent = PersonIdentNumber("12345678911")
            val firstQuestionResponse = QuestionResponse(
                "FREMTIDIG_SITUASJON",
                "Hvordan ser du for deg din situasjon når sykepengene er slutt?",
                "TILBAKE_HOS_ARBEIDSGIVER",
                "Jeg skal tilbake til min arbeidsgiver",
            )
            val secondQuestionResponse = QuestionResponse(
                "BEHOV_FOR_OPPFOLGING",
                "Trenger du hjelp fra NAV?",
                "JA",
                "Ja",
            )

            responseDao.saveFormResponse(
                personIdent,
                listOf(firstQuestionResponse, secondQuestionResponse),
                FormType.SEN_OPPFOLGING_V2,
                LocalDateTime.now(),
                null,
            )

            val formResponse = responseDao.findLatestFormResponse(
                personIdent,
                FormType.SEN_OPPFOLGING_V2,
                LocalDate.now().minusDays(1),
            )

            checkNotNull(formResponse)
            checkNotNull(formResponse.questionResponses)

            formResponse.questionResponses.shouldContain(firstQuestionResponse)
            formResponse.questionResponses.shouldContain(secondQuestionResponse)
        }
    }
}
