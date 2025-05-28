package no.nav.syfo.senoppfolging.v2

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.auth.getFnr
import no.nav.syfo.besvarelse.database.ResponseDao
import no.nav.syfo.besvarelse.database.domain.FormResponse
import no.nav.syfo.besvarelse.database.domain.FormType.SEN_OPPFOLGING_V2
import no.nav.syfo.besvarelse.database.domain.QuestionResponse
import no.nav.syfo.dokarkiv.DokarkivClient
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.metric.Metric
import no.nav.syfo.oppfolgingstilfelle.IsOppfolgingstilfelleClient
import no.nav.syfo.oppfolgingstilfelle.Oppfolgingstilfelle
import no.nav.syfo.senoppfolging.exception.AlreadyRespondedException
import no.nav.syfo.senoppfolging.exception.NoAccessToSenOppfolgingException
import no.nav.syfo.senoppfolging.exception.NoUtsendtVarselException
import no.nav.syfo.senoppfolging.kafka.SenOppfolgingSvarKafkaProducer
import no.nav.syfo.senoppfolging.service.SenOppfolgingService
import no.nav.syfo.senoppfolging.v2.domain.ResponseStatus.NO_RESPONSE
import no.nav.syfo.senoppfolging.v2.domain.ResponseStatus.TRENGER_IKKE_OPPFOLGING
import no.nav.syfo.senoppfolging.v2.domain.ResponseStatus.TRENGER_OPPFOLGING
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingDTOV2
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionTypeV2.BEHOV_FOR_OPPFOLGING
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionV2
import no.nav.syfo.senoppfolging.v2.domain.toQuestionResponse
import no.nav.syfo.syfoopppdfgen.PdfgenService
import no.nav.syfo.sykepengedagerinformasjon.service.SykepengedagerInformasjonService
import no.nav.syfo.varsel.UtsendtVarselEsyfovarselCopy
import no.nav.syfo.varsel.VarselService
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SenOppfolgingControllerV2Test :
    DescribeSpec(
        {
            val tokenValidationContextHolder = mockk<TokenValidationContextHolder>(relaxed = true)
            val tokenValidator = mockk<TokenValidator>(relaxed = true)
            val varselService = mockk<VarselService>(relaxed = true)
            val metric = mockk<Metric>(relaxed = true)
            val responseDao = mockk<ResponseDao>(relaxed = true)
            val senOppfolgingSvarKafkaProducer = mockk<SenOppfolgingSvarKafkaProducer>(relaxed = true)
            val sykepengedagerInformasjonService = mockk<SykepengedagerInformasjonService>(relaxed = true)
            val syfoopfpdfgenService = mockk<PdfgenService>(relaxed = true)
            val dokarkivClient = mockk<DokarkivClient>(relaxed = true)
            val isOppfolgingstilfelleClient = mockk<IsOppfolgingstilfelleClient>(relaxed = true)

            val senOppfolgingService =
                SenOppfolgingService(
                    sykepengedagerInformasjonService = sykepengedagerInformasjonService,
                    isOppfolgingstilfelleClient = isOppfolgingstilfelleClient,
                    varselService = varselService,
                    responseDao = responseDao,
                    metric = metric,
                    senOppfolgingSvarKafkaProducer = senOppfolgingSvarKafkaProducer,
                    dokarkivClient = dokarkivClient,
                    syfoopfpdfgenService = syfoopfpdfgenService,
                )

            val controller =
                SenOppfolgingControllerV2(
                    merOppfolgingFrontendClientId = "merOppfolgingFrontendClientId",
                    esyfoProxyClientId = "esyfoProxyClientId",
                    tokenValidationContextHolder = tokenValidationContextHolder,
                    senOppfolgingService = senOppfolgingService,
                ).apply {
                    this.tokenValidator = tokenValidator
                }

            val ansattFnr = "12345678910"

            val questionResponse = QuestionResponse("Test type", "Test question", "Text", "Test answer")
            val questionResponseReq =
                SenOppfolgingDTOV2(
                    listOf(
                        SenOppfolgingQuestionV2(BEHOV_FOR_OPPFOLGING, "Test question", "Text", "Test answer"),
                    ),
                )
            val formResponse =
                FormResponse(
                    UUID.randomUUID(),
                    PersonIdentNumber(ansattFnr),
                    LocalDateTime.now().minusDays(1),
                    SEN_OPPFOLGING_V2,
                )

            val oppfolgingstilfelleActive: List<Oppfolgingstilfelle> =
                listOf(Oppfolgingstilfelle(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10)))
            val oppfolgingstilfelleEnded16DaysAgo: List<Oppfolgingstilfelle> =
                listOf(Oppfolgingstilfelle(LocalDate.now().minusDays(20), LocalDate.now().minusDays(16)))
            val oppfolgingstilfelleEnded17DaysAgo: List<Oppfolgingstilfelle> =
                listOf(Oppfolgingstilfelle(LocalDate.now().minusDays(30), LocalDate.now().minusDays(17)))

            val varselUuid = UUID.randomUUID()
            val varselSentAt = LocalDateTime.now().minusDays(20)

            beforeTest {
                clearAllMocks()

                every { tokenValidator.validateTokenXClaims().getFnr() } returns ansattFnr
                every { varselService.getUtsendtVarsel(ansattFnr) } returns
                    UtsendtVarselEsyfovarselCopy(varselUuid, ansattFnr, varselSentAt)
                every {
                    isOppfolgingstilfelleClient.getOppfolgingstilfeller(any())
                } returns oppfolgingstilfelleActive
                every { responseDao.findResponseByVarselId(any()) } returns null
                every { responseDao.findLatestFormResponse(any(), any(), any()) } returns null
            }

            describe("Form submission") {
                it("should save form response when oppfolgingstilfelle is active") {
                    val response = controller.submitForm(
                        questionResponseReq,
                    )

                    verify(exactly = 1) {
                        responseDao.saveFormResponse(
                            personIdent = PersonIdentNumber(ansattFnr),
                            questionResponses = questionResponseReq.senOppfolgingFormV2.map { it.toQuestionResponse() },
                            formType = SEN_OPPFOLGING_V2,
                            createdAt = any<LocalDateTime>(),
                            utsendtVarselUUID = varselUuid,
                        )
                    }
                    verify(exactly = 1) {
                        senOppfolgingSvarKafkaProducer.publishResponse(
                            match(
                                {
                                    it.personIdent == ansattFnr &&
                                        it.response == questionResponseReq.senOppfolgingFormV2 &&
                                        it.varselId == varselUuid
                                },
                            ),
                        )
                    }
                    assertTrue(response.statusCode.isSameCodeAs(HttpStatus.CREATED))
                }
                it("should save form response when oppfolgingstilfelle ended 16 days ago") {
                    every {
                        isOppfolgingstilfelleClient.getOppfolgingstilfeller(any())
                    } returns oppfolgingstilfelleEnded16DaysAgo
                    controller.submitForm(
                        questionResponseReq,
                    )

                    verify(exactly = 1) {
                        responseDao.saveFormResponse(
                            personIdent = PersonIdentNumber(ansattFnr),
                            questionResponses = questionResponseReq.senOppfolgingFormV2.map { it.toQuestionResponse() },
                            formType = SEN_OPPFOLGING_V2,
                            createdAt = any<LocalDateTime>(),
                            utsendtVarselUUID = varselUuid,
                        )
                    }
                    verify(exactly = 1) {
                        senOppfolgingSvarKafkaProducer.publishResponse(
                            match(
                                {
                                    it.personIdent == ansattFnr &&
                                        it.response == questionResponseReq.senOppfolgingFormV2 &&
                                        it.varselId == varselUuid
                                },
                            ),
                        )
                    }
                }

                describe("should throw erros when") {
                    it("response with varsel uuid exist") {
                        every { responseDao.findResponseByVarselId(varselUuid) } returns formResponse

                        shouldThrow<AlreadyRespondedException> {
                            controller.submitForm(
                                questionResponseReq,
                            )
                        }
                    }
                    it("response submitted after utsendt varsel exist") {
                        every {
                            responseDao.findLatestFormResponse(
                                PersonIdentNumber(ansattFnr),
                                SEN_OPPFOLGING_V2,
                                varselSentAt.toLocalDate(),
                            )
                        } returns formResponse

                        shouldThrow<AlreadyRespondedException> {
                            controller.submitForm(
                                questionResponseReq,
                            )
                        }
                    }
                    it("no valid utsendt varsel") {
                        every { varselService.getUtsendtVarsel(ansattFnr) } returns null

                        shouldThrow<NoUtsendtVarselException> {
                            controller.submitForm(
                                questionResponseReq,
                            )
                        }
                    }
                    it("oppfolgingstilfelle ended more than 16 days ago") {
                        every {
                            isOppfolgingstilfelleClient.getOppfolgingstilfeller(any())
                        } returns oppfolgingstilfelleEnded17DaysAgo

                        shouldThrow<NoAccessToSenOppfolgingException> {
                            controller.submitForm(
                                questionResponseReq,
                            )
                        }
                    }

                    it("should archive (dokarkiv) submitted answers for all users") {
                        every {
                            syfoopfpdfgenService.getSenOppfolgingReceiptPdf(ansattFnr, any(), any())
                        } returns ByteArray(1)

                        controller.submitForm(
                            questionResponseReq,
                        )

                        verify(exactly = 1) {
                            responseDao.saveFormResponse(any(), any(), SEN_OPPFOLGING_V2, any(), any())
                        }
                        verify(
                            exactly = 1,
                        ) { dokarkivClient.postDocumentsForsendelseToDokarkiv(ansattFnr, any(), any(), any()) }
                    }
                }
            }

            describe("Get status should") {
                it("return TRENGER_IKKE_OPPFOLGING when user has answered Nei") {
                    every { responseDao.findLatestFormResponse(any(), SEN_OPPFOLGING_V2, any()) } returns
                        formResponse.copy(
                            questionResponses =
                            mutableListOf(QuestionResponse(BEHOV_FOR_OPPFOLGING.name, "Text", "NEI", "Nei")),
                        )

                    val status = controller.status()

                    status.responseStatus shouldBe TRENGER_IKKE_OPPFOLGING
                }

                it("return TRENGER_OPPFOLGING when user has answered Ja") {
                    every { responseDao.findLatestFormResponse(any(), SEN_OPPFOLGING_V2, any()) } returns
                        formResponse.copy(
                            questionResponses =
                            mutableListOf(QuestionResponse(BEHOV_FOR_OPPFOLGING.name, "Text", "JA", "Ja")),
                        )

                    val status = controller.status()

                    status.responseStatus shouldBe TRENGER_OPPFOLGING
                }

                it("return NO_RESPONSE when user hasn't answered") {
                    val status = controller.status()

                    status.responseStatus shouldBe NO_RESPONSE
                }

                it("return hasAccessToSenOppfolging=true") {
                    val status = controller.status()

                    status.hasAccessToSenOppfolging shouldBe true
                }

                it("return hasAccessToSenOppfolging=false when oppfolgingstilfelle ended more than 16 days ago") {
                    every {
                        isOppfolgingstilfelleClient.getOppfolgingstilfeller(any())
                    } returns oppfolgingstilfelleEnded17DaysAgo

                    val status = controller.status()

                    status.hasAccessToSenOppfolging shouldBe false
                }

                it("return hasAccessToSenOppfolging=false when no utsendt varsel is found") {
                    every {
                        varselService.getUtsendtVarsel(ansattFnr)
                    } returns null

                    val status = controller.status()

                    status.hasAccessToSenOppfolging shouldBe false
                }
                it("return response if response with varsel uuid exist") {
                    every { responseDao.findResponseByVarselId(varselUuid) } returns
                        formResponse.copy(questionResponses = mutableListOf(questionResponse))

                    val status = controller.status()

                    status.response shouldBe mutableListOf(questionResponse)
                }
                it("response submitted after utsendt varsel exist") {
                    every {
                        responseDao.findLatestFormResponse(
                            PersonIdentNumber(ansattFnr),
                            SEN_OPPFOLGING_V2,
                            varselSentAt.toLocalDate(),
                        )
                    } returns formResponse

                    every { responseDao.findResponseByVarselId(varselUuid) } returns
                        formResponse.copy(questionResponses = mutableListOf(questionResponse))

                    val status = controller.status()

                    status.response shouldBe mutableListOf(questionResponse)
                }
            }
        },
    )
