package no.nav.syfo.senoppfolging.v2

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.NoAccess
import no.nav.syfo.besvarelse.database.ResponseDao
import no.nav.syfo.besvarelse.database.domain.FormResponse
import no.nav.syfo.besvarelse.database.domain.FormType
import no.nav.syfo.besvarelse.database.domain.QuestionResponse
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionTypeV2.BEHOV_FOR_OPPFOLGING
import no.nav.syfo.veiledertilgang.VeilederTilgangClient
import org.springframework.http.HttpStatusCode
import java.time.LocalDateTime
import java.util.UUID

class SenOppfolgingVeilederADControllerV2Test : DescribeSpec(
    {
        val tokenValidationContextHolder = mockk<TokenValidationContextHolder>(relaxed = true)
        val responseDao = mockk<ResponseDao>(relaxed = true)
        val veilederTilgangClient = mockk<VeilederTilgangClient>(relaxed = true)

        val controller = SenOppfolgingVeilederADControllerV2(
            tokenValidationContextHolder = tokenValidationContextHolder,
            veilederTilgangClient = veilederTilgangClient,
            responseDao = responseDao,
        )

        val fnr = "12345678910"
        val personIdent = PersonIdentNumber(fnr)

        beforeTest {
            clearAllMocks()
        }

        describe("Get form response") {
            it("returns successful response when veileder has access to person") {
                every { veilederTilgangClient.hasVeilederTilgangToPerson(any(), any()) } returns true

                val response = controller.getFormResponse(fnr)
                TestCase.assertTrue(response.statusCode.is2xxSuccessful)
            }

            it("returns no content when veileder has access to person but person has no form response") {
                every { veilederTilgangClient.hasVeilederTilgangToPerson(any(), any()) } returns true
                every { responseDao.findLatestFormResponse(any(), any()) } returns null

                val response = controller.getFormResponse(fnr)
                TestCase.assertTrue(response.statusCode.isSameCodeAs(HttpStatusCode.valueOf(204)))
                TestCase.assertNull(response.body)
            }

            it("returns OK response when veileder has access to person with form response") {
                every { veilederTilgangClient.hasVeilederTilgangToPerson(any(), any()) } returns true

                val responseUUID = UUID.randomUUID()
                val responseCreatedAt = LocalDateTime.now()

                every { responseDao.findLatestFormResponse(any(), any()) } returns FormResponse(
                    uuid = responseUUID,
                    personIdent = personIdent,
                    createdAt = responseCreatedAt,
                    formType = FormType.SEN_OPPFOLGING_V2,
                    questionResponses = mutableListOf(
                        QuestionResponse(BEHOV_FOR_OPPFOLGING.name, "", "JA", "Ja")
                    )
                )

                val response = controller.getFormResponse(fnr)
                TestCase.assertTrue(response.statusCode.isSameCodeAs(HttpStatusCode.valueOf(200)))

                val responseDTOV2 = response.body!!
                TestCase.assertEquals(responseUUID.toString(), responseDTOV2.uuid)
                TestCase.assertEquals(fnr, responseDTOV2.personIdent)
                TestCase.assertEquals(responseCreatedAt, responseDTOV2.createdAt)
                TestCase.assertEquals(FormType.SEN_OPPFOLGING_V2.name, responseDTOV2.formType)
                TestCase.assertEquals(1, responseDTOV2.questionResponses.size)
                val questionResponseDTO = responseDTOV2.questionResponses.first()
                TestCase.assertEquals(BEHOV_FOR_OPPFOLGING.name, questionResponseDTO.questionType)
                TestCase.assertEquals("JA", questionResponseDTO.answerType)
            }

            it("throws NoAccess when veileder denied access to person") {
                every { veilederTilgangClient.hasVeilederTilgangToPerson(any(), any()) } returns false

                val exception = shouldThrow<NoAccess> {
                    controller.getFormResponse(fnr)
                }
                TestCase.assertEquals("Veileder har ikke tilgang til person", exception.message)
            }
        }
    },
)
