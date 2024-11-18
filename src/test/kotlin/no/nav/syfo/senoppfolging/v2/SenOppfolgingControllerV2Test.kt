package no.nav.syfo.senoppfolging.v2

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.auth.getFnr
import no.nav.syfo.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.behandlendeenhet.domain.BehandlendeEnhet
import no.nav.syfo.besvarelse.database.ResponseDao
import no.nav.syfo.besvarelse.database.domain.FormResponse
import no.nav.syfo.besvarelse.database.domain.FormType.SEN_OPPFOLGING_V2
import no.nav.syfo.besvarelse.database.domain.QuestionResponse
import no.nav.syfo.dokarkiv.DokarkivClient
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.metric.Metric
import no.nav.syfo.oppfolgingstilfelle.IsOppfolgingstilfelleClient
import no.nav.syfo.oppfolgingstilfelle.Oppfolgingstilfelle
import no.nav.syfo.senoppfolging.AlreadyRespondedException
import no.nav.syfo.senoppfolging.NoAccessToSenOppfolgingException
import no.nav.syfo.senoppfolging.NoUtsendtVarselException
import no.nav.syfo.senoppfolging.kafka.SenOppfolgingSvarKafkaProducer
import no.nav.syfo.senoppfolging.v2.domain.ResponseStatus.NO_RESPONSE
import no.nav.syfo.senoppfolging.v2.domain.ResponseStatus.TRENGER_IKKE_OPPFOLGING
import no.nav.syfo.senoppfolging.v2.domain.ResponseStatus.TRENGER_OPPFOLGING
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingDTOV2
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionTypeV2.BEHOV_FOR_OPPFOLGING
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionV2
import no.nav.syfo.senoppfolging.v2.domain.toQuestionResponse
import no.nav.syfo.syfoopppdfgen.PdfgenService
import no.nav.syfo.sykepengedagerinformasjon.domain.PSykepengedagerInformasjon
import no.nav.syfo.sykepengedagerinformasjon.service.SykepengedagerInformasjonService
import no.nav.syfo.varsel.UtsendtVarselEsyfovarselCopy
import no.nav.syfo.varsel.VarselService
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class SenOppfolgingControllerV2Test :
    DescribeSpec(
        {
            val tokenValidationContextHolder = mockk<TokenValidationContextHolder>(relaxed = true)
            val tokenValidator = mockk<TokenValidator>(relaxed = true)
            val varselService = mockk<VarselService>(relaxed = true)
            val metric = mockk<Metric>(relaxed = true)
            val responseDao = mockk<ResponseDao>(relaxed = true)
            val behandlendeEnhetClient = mockk<BehandlendeEnhetClient>(relaxed = true)
            val senOppfolgingSvarKafkaProducer = mockk<SenOppfolgingSvarKafkaProducer>(relaxed = true)
            val sykepengedagerInformasjonService = mockk<SykepengedagerInformasjonService>(relaxed = true)
            val syfoopfpdfgenService = mockk<PdfgenService>(relaxed = true)
            val dokarkivClient = mockk<DokarkivClient>(relaxed = true)
            val isOppfolgingstilfelleClient = mockk<IsOppfolgingstilfelleClient>(relaxed = true)

            val controller =
                SenOppfolgingControllerV2(
                    merOppfolgingFrontendClientId = "merOppfolgingFrontendClientId",
                    esyfoProxyClientId = "esyfoProxyClientId",
                    tokenValidationContextHolder = tokenValidationContextHolder,
                    varselService = varselService,
                    metric = metric,
                    responseDao = responseDao,
                    senOppfolgingSvarKafkaProducer = senOppfolgingSvarKafkaProducer,
                    sykepengedagerInformasjonService = sykepengedagerInformasjonService,
                    syfoopfpdfgenService = syfoopfpdfgenService,
                    dokarkivClient = dokarkivClient,
                    isOppfolgingstilfelleClient = isOppfolgingstilfelleClient,
                ).apply {
                    this.tokenValidator = tokenValidator
                }

            val ansattFnr = "12345678910"

            val formResponseDb =
                listOf(
                    QuestionResponse(BEHOV_FOR_OPPFOLGING.name, "Test question", "Text", "Test answer"),
                )
            val formResponse =
                SenOppfolgingDTOV2(
                    listOf(
                        SenOppfolgingQuestionV2(BEHOV_FOR_OPPFOLGING, "Test question", "Text", "Test answer"),
                    ),
                )

            val oppfolgingstilfelleActive: List<Oppfolgingstilfelle> =
                listOf(Oppfolgingstilfelle(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10)))
            val oppfolgingstilfelleEnded16DaysAgo: List<Oppfolgingstilfelle> =
                listOf(Oppfolgingstilfelle(LocalDate.now().minusDays(20), LocalDate.now().minusDays(16)))
            val oppfolgingstilfelleEnded17DaysAgo: List<Oppfolgingstilfelle> =
                listOf(Oppfolgingstilfelle(LocalDate.now().minusDays(30), LocalDate.now().minusDays(17)))

            val varselUuid = UUID.randomUUID()

            beforeTest {
                clearAllMocks()

                every { tokenValidator.validateTokenXClaims().getFnr() } returns ansattFnr
                every { varselService.getUtsendtVarsel(ansattFnr) } returns
                    UtsendtVarselEsyfovarselCopy(varselUuid, ansattFnr, LocalDateTime.now())
                every {
                    isOppfolgingstilfelleClient.getOppfolgingstilfeller(any())
                } returns oppfolgingstilfelleActive
                every { responseDao.find(any(), SEN_OPPFOLGING_V2, any()) } returns emptyList()
            }

            describe("Form submission") {
                it("should save form response when oppfolgingstilfelle is active") {
                    controller.submitForm(
                        formResponse,
                    )

                    verify(exactly = 1) {
                        responseDao.saveFormResponse(
                            personIdent = PersonIdentNumber(ansattFnr),
                            questionResponses = formResponse.senOppfolgingFormV2.map { it.toQuestionResponse() },
                            formType = SEN_OPPFOLGING_V2,
                            createdAt = any<LocalDateTime>(),
                            utsendtVarselUUID = varselUuid,
                        )
                    }
                    verify(exactly = 1) {
                        senOppfolgingSvarKafkaProducer.publishResponse(
                            match({
                                it.personIdent == ansattFnr &&
                                    it.response == formResponse.senOppfolgingFormV2 &&
                                    it.varselId == varselUuid
                            }),
                        )
                    }
                }
                it("should save form response when oppfolgingstilfelle ended 16 days ago") {
                    every {
                        isOppfolgingstilfelleClient.getOppfolgingstilfeller(any())
                    } returns oppfolgingstilfelleEnded16DaysAgo
                    controller.submitForm(
                        formResponse,
                    )

                    verify(exactly = 1) {
                        responseDao.saveFormResponse(
                            personIdent = PersonIdentNumber(ansattFnr),
                            questionResponses = formResponse.senOppfolgingFormV2.map { it.toQuestionResponse() },
                            formType = SEN_OPPFOLGING_V2,
                            createdAt = any<LocalDateTime>(),
                            utsendtVarselUUID = varselUuid,
                        )
                    }
                    verify(exactly = 1) {
                        senOppfolgingSvarKafkaProducer.publishResponse(
                            match({
                                it.personIdent == ansattFnr &&
                                    it.response == formResponse.senOppfolgingFormV2 &&
                                    it.varselId == varselUuid
                            }),
                        )
                    }
                }

                describe("should throw erros when") {
                    it("response already exist") {
                        every { responseDao.find(any(), any(), any()) } returns formResponseDb

                        shouldThrow<AlreadyRespondedException> {
                            controller.submitForm(
                                formResponse,
                            )
                        }
                    }
                    it("no valid utsendt varsel") {
                        every { varselService.getUtsendtVarsel(ansattFnr) } returns null

                        shouldThrow<NoUtsendtVarselException> {
                            controller.submitForm(
                                formResponse,
                            )
                        }
                    }
                    it("oppfolgingstilfelle ended more than 16 days ago") {
                        every {
                            isOppfolgingstilfelleClient.getOppfolgingstilfeller(any())
                        } returns oppfolgingstilfelleEnded17DaysAgo

                        shouldThrow<NoAccessToSenOppfolgingException> {
                            controller.submitForm(
                                formResponse,
                            )
                        }
                    }

                    it("should archive (dokarkiv) submitted answers for all users") {
                        every { behandlendeEnhetClient.getBehandlendeEnhet(ansattFnr) } returns
                            BehandlendeEnhet(
                                "0314",
                                "Testkontor",
                            )
                        every { syfoopfpdfgenService.getSenOppfolgingReceiptPdf(ansattFnr, any()) } returns ByteArray(1)

                        controller.submitForm(
                            formResponse,
                        )

                        verify(exactly = 1) {
                            responseDao.saveFormResponse(any(), any(), SEN_OPPFOLGING_V2, any(), any())
                        }
                        verify(exactly = 1) { dokarkivClient.postDocumentToDokarkiv(ansattFnr, any(), any()) }
                    }
                }
            }

            describe("Get status should") {
                it("return TRENGER_IKKE_OPPFOLGING when user has answered Nei") {
                    every { responseDao.findLatestFormResponse(any(), SEN_OPPFOLGING_V2, any()) } returns
                        FormResponse(
                            UUID.randomUUID(),
                            PersonIdentNumber(ansattFnr),
                            LocalDateTime.now().minusDays(1),
                            SEN_OPPFOLGING_V2,
                        ).apply {
                            questionResponses.add(QuestionResponse(BEHOV_FOR_OPPFOLGING.name, "", "NEI", "Nei"))
                        }

                    val status = controller.status()

                    status.responseStatus shouldBe TRENGER_IKKE_OPPFOLGING
                }

                it("return TRENGER_OPPFOLGING when user has answered Ja") {
                    every { responseDao.findLatestFormResponse(any(), SEN_OPPFOLGING_V2, any()) } returns
                        FormResponse(
                            UUID.randomUUID(),
                            PersonIdentNumber(ansattFnr),
                            LocalDateTime.now().minusDays(1),
                            SEN_OPPFOLGING_V2,
                        ).apply {
                            questionResponses.add(QuestionResponse(BEHOV_FOR_OPPFOLGING.name, "", "JA", "Ja"))
                        }

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
            }
        },
    )
