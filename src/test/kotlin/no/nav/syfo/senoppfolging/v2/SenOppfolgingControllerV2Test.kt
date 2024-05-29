package no.nav.syfo.senoppfolging.v2

import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.auth.getFnr
import no.nav.syfo.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.besvarelse.database.ResponseDao
import no.nav.syfo.besvarelse.database.domain.FormType.SEN_OPPFOLGING_V2
import no.nav.syfo.besvarelse.database.domain.QuestionResponse
import no.nav.syfo.metric.Metric
import no.nav.syfo.senoppfolging.kafka.KSenOppfolgingSvarDTOV2
import no.nav.syfo.senoppfolging.kafka.SenOppfolgingSvarKafkaProducer
import no.nav.syfo.senoppfolging.v2.domain.ResponseStatus.NO_RESPONSE
import no.nav.syfo.senoppfolging.v2.domain.ResponseStatus.TRENGER_IKKE_OPPFOLGING
import no.nav.syfo.senoppfolging.v2.domain.ResponseStatus.TRENGER_OPPFOLGING
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingDTOV2
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionTypeV2.ONSKER_OPPFOLGING
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionV2
import no.nav.syfo.varsel.VarselService

class SenOppfolgingControllerV2Test : DescribeSpec(
    {
        val tokenValidationContextHolder = mockk<TokenValidationContextHolder>(relaxed = true)
        val tokenValidator = mockk<TokenValidator>(relaxed = true)
        val varselService = mockk<VarselService>(relaxed = true)
        val metric = mockk<Metric>(relaxed = true)
        val responseDao = mockk<ResponseDao>(relaxed = true)
        val behandlendeEnhetClient = mockk<BehandlendeEnhetClient>(relaxed = true)
        val senOppfolgingSvarKafkaProducer = mockk<SenOppfolgingSvarKafkaProducer>(relaxed = true)

        val controller = SenOppfolgingControllerV2(
            merOppfolgingFrontendClientId = "merOppfolgingFrontendClientId",
            tokenValidationContextHolder = tokenValidationContextHolder,
            varselService = varselService,
            metric = metric,
            responseDao = responseDao,
            behandlendeEnhetClient = behandlendeEnhetClient,
            pilotEnabledForEnvironment = true,
            senOppfolgingSvarKafkaProducer = senOppfolgingSvarKafkaProducer,

        ).apply {
            this.tokenValidator = tokenValidator
        }

        val ansattFnr = "12345678910"

        beforeTest {
            clearAllMocks()
        }

        describe("Form submission") {
            it("should save form response") {
                every { tokenValidator.validateTokenXClaims().getFnr() } returns ansattFnr
                val responses = listOf(
                    SenOppfolgingQuestionV2(ONSKER_OPPFOLGING, "Hei", "JA", "Ja"),
                )
                controller.submitForm(
                    SenOppfolgingDTOV2(
                        responses,
                    ),
                )
                verify(exactly = 1) {
                    responseDao.saveFormResponse(any(), any(), SEN_OPPFOLGING_V2, any())
                }
                verify(exactly = 1) {
                    senOppfolgingSvarKafkaProducer.publishResponse(
                        any<KSenOppfolgingSvarDTOV2>()
                    )
                }
            }
        }

        describe("Get status") {
            it("Should return TRENGER_IKKE_OPPFOLGING when user has answered Nei") {
                every { tokenValidator.validateTokenXClaims().getFnr() } returns ansattFnr
                every { responseDao.find(any(), SEN_OPPFOLGING_V2, any()) } returns listOf(
                    QuestionResponse(ONSKER_OPPFOLGING.name, "", "NEI", "Nei"),
                )
                val status = controller.status()
                TestCase.assertEquals(TRENGER_IKKE_OPPFOLGING, status.responseStatus)
            }

            it("Should return TRENGER_OPPFOLGING when user has answered Ja") {
                every { tokenValidator.validateTokenXClaims().getFnr() } returns ansattFnr
                every { responseDao.find(any(), SEN_OPPFOLGING_V2, any()) } returns listOf(
                    QuestionResponse(ONSKER_OPPFOLGING.name, "", "JA", "Ja"),
                )
                val status = controller.status()
                TestCase.assertEquals(TRENGER_OPPFOLGING, status.responseStatus)
            }

            it("Should return NO_RESPONSE when user hasn't answered") {
                every { tokenValidator.validateTokenXClaims().getFnr() } returns ansattFnr
                every { responseDao.find(any(), SEN_OPPFOLGING_V2, any()) } returns emptyList()
                val status = controller.status()
                TestCase.assertEquals(NO_RESPONSE, status.responseStatus)
            }
        }
    },
)
