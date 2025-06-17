package no.nav.syfo.besvarelse.database

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.BEFORE_CLASS
import no.nav.syfo.LocalApplication
import no.nav.syfo.besvarelse.database.domain.FormType
import no.nav.syfo.besvarelse.database.domain.QuestionResponse
import no.nav.syfo.domain.PersonIdentNumber
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
@AutoConfigureEmbeddedDatabase(provider = ZONKY, refresh = BEFORE_CLASS)
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

        it("Find latest response created at or before from date") {
            val personIdent = PersonIdentNumber("12345678911")
            val questionResponse =
                QuestionResponse(
                    "FREMTIDIG_SITUASJON",
                    "Hvordan ser du for deg din situasjon når sykepengene er slutt?",
                    "TILBAKE_HOS_ARBEIDSGIVER",
                    "Jeg skal tilbake til min arbeidsgiver",
                )
            responseDao.saveFormResponse(
                personIdent = personIdent,
                questionResponses = listOf(questionResponse),
                formType = FormType.SEN_OPPFOLGING_V2,
                createdAt = LocalDateTime.now().minusDays(2),
                utsendtVarselUUID = null,
            )

            val formResponse =
                responseDao.findLatestFormResponse(
                    personIdent = personIdent,
                    formType = FormType.SEN_OPPFOLGING_V2,
                    from = LocalDate.now().minusDays(2),
                )

            checkNotNull(formResponse)
            checkNotNull(formResponse.questionResponses)

            val question = formResponse.questionResponses[0]

            assertEquals(questionResponse.questionType, question.questionType)
            assertEquals(questionResponse.questionText, question.questionText)
            assertEquals(questionResponse.answerType, question.answerType)
            assertEquals(questionResponse.answerText, question.answerText)
        }

        it("Find latest response (no from date)") {
            val personIdent = PersonIdentNumber("12345678911")
            val firstQuestionResponse =
                QuestionResponse(
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

            val formResponse =
                responseDao.findLatestFormResponse(
                    personIdent = personIdent,
                    formType = FormType.SEN_OPPFOLGING_V2,
                )
            checkNotNull(formResponse)
        }

        it("Find latest response") {
            val personIdent = PersonIdentNumber("12345678911")
            val firstQuestionResponse =
                QuestionResponse(
                    "FREMTIDIG_SITUASJON",
                    "Hvordan ser du for deg din situasjon når sykepengene er slutt?",
                    "TILBAKE_HOS_ARBEIDSGIVER",
                    "Jeg skal tilbake til min arbeidsgiver",
                )
            val latestQuestionResponse =
                QuestionResponse(
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

            val formResponse =
                responseDao.findLatestFormResponse(
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

        it("should find a form response with questions in the same order as they were saved") {
            val personIdent = PersonIdentNumber("12345678911")
            val firstQuestionResponse =
                QuestionResponse(
                    "FREMTIDIG_SITUASJON",
                    "Hvilken situasjon tror du at du er i når sykepengene har tatt slutt?",
                    "TILBAKE_HOS_ARBEIDSGIVER",
                    "Jeg er frisk og tilbake hos arbeidsgiver",
                )
            val secondQuestionResponse =
                QuestionResponse(
                    "BEHOV_FOR_OPPFOLGING",
                    "Ønsker du å be om oppfølging?",
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

            val formResponse =
                responseDao.findLatestFormResponse(
                    personIdent,
                    FormType.SEN_OPPFOLGING_V2,
                    LocalDate.now().minusDays(1),
                )

            checkNotNull(formResponse)
            checkNotNull(formResponse.questionResponses)

            formResponse.questionResponses[0].questionType shouldBe firstQuestionResponse.questionType
            formResponse.questionResponses[1].questionType shouldBe secondQuestionResponse.questionType
        }

        it("find response with varsel id") {
            val personIdent = PersonIdentNumber("12345678911")
            val questionResponse =
                QuestionResponse(
                    "FREMTIDIG_SITUASJON",
                    "Hvordan ser du for deg din situasjon når sykepengene er slutt?",
                    "TILBAKE_HOS_ARBEIDSGIVER",
                    "Jeg skal tilbake til min arbeidsgiver",
                )
            val varselId = UUID.randomUUID()

            responseDao.saveFormResponse(
                personIdent,
                listOf(questionResponse),
                FormType.SEN_OPPFOLGING_V2,
                LocalDateTime.now().minusDays(1),
                varselId,
            )

            val formResponse =
                responseDao.findResponseByVarselId(
                    varselId,
                )

            checkNotNull(formResponse)
            checkNotNull(formResponse.questionResponses)

            val question = formResponse.questionResponses[0]

            assertEquals(questionResponse.questionType, question.questionType)
            assertEquals(questionResponse.questionText, question.questionText)
            assertEquals(questionResponse.answerType, question.answerType)
            assertEquals(questionResponse.answerText, question.answerText)
        }
    }
}
