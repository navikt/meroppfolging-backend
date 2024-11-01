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
import no.nav.syfo.maksdato.EsyfovarselClient
import no.nav.syfo.metric.Metric
import no.nav.syfo.senoppfolging.NoUtsendtVarselException
import no.nav.syfo.senoppfolging.kafka.KSenOppfolgingSvarDTO
import no.nav.syfo.senoppfolging.kafka.SenOppfolgingSvarKafkaProducer
import no.nav.syfo.senoppfolging.v2.domain.ResponseStatus.NO_RESPONSE
import no.nav.syfo.senoppfolging.v2.domain.ResponseStatus.TRENGER_IKKE_OPPFOLGING
import no.nav.syfo.senoppfolging.v2.domain.ResponseStatus.TRENGER_OPPFOLGING
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingDTOV2
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionTypeV2.BEHOV_FOR_OPPFOLGING
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionV2
import no.nav.syfo.syfoopppdfgen.PdfgenService
import no.nav.syfo.varsel.UtsendtVarselEsyfovarselCopy
import no.nav.syfo.varsel.VarselService
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
            val esyfovarselClient = mockk<EsyfovarselClient>(relaxed = true)
            val syfoopfpdfgenService = mockk<PdfgenService>(relaxed = true)
            val dokarkivClient = mockk<DokarkivClient>(relaxed = true)

            val controller =
                SenOppfolgingControllerV2(
                    merOppfolgingFrontendClientId = "merOppfolgingFrontendClientId",
                    esyfoProxyClientId = "esyfoProxyClientId",
                    tokenValidationContextHolder = tokenValidationContextHolder,
                    varselService = varselService,
                    metric = metric,
                    responseDao = responseDao,
                    senOppfolgingSvarKafkaProducer = senOppfolgingSvarKafkaProducer,
                    esyfovarselClient = esyfovarselClient,
                    syfoopfpdfgenService = syfoopfpdfgenService,
                    dokarkivClient = dokarkivClient,
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
                    every { varselService.getUtsendtVarsel(ansattFnr) } returns
                        UtsendtVarselEsyfovarselCopy(UUID.randomUUID(), ansattFnr, LocalDateTime.now())
                    val responses =
                        listOf(
                            SenOppfolgingQuestionV2(BEHOV_FOR_OPPFOLGING, "Hei", "JA", "Ja"),
                        )
                    controller.submitForm(
                        SenOppfolgingDTOV2(
                            responses,
                        ),
                    )
                    verify(exactly = 1) {
                        responseDao.saveFormResponse(any(), any(), SEN_OPPFOLGING_V2, any(), any())
                    }
                    verify(exactly = 1) {
                        senOppfolgingSvarKafkaProducer.publishResponse(
                            any<KSenOppfolgingSvarDTO>(),
                        )
                    }
                }
                it("should throw error when no valid utsendt varsel") {
                    every { tokenValidator.validateTokenXClaims().getFnr() } returns ansattFnr
                    every { varselService.getUtsendtVarsel(ansattFnr) } returns null
                    val responses =
                        listOf(
                            SenOppfolgingQuestionV2(BEHOV_FOR_OPPFOLGING, "Hei", "JA", "Ja"),
                        )

                    shouldThrow<NoUtsendtVarselException> {
                        controller.submitForm(
                            SenOppfolgingDTOV2(
                                responses,
                            ),
                        )
                    }
                }
            }

            describe("Get status") {
                it("Should return TRENGER_IKKE_OPPFOLGING when user has answered Nei") {
                    every { tokenValidator.validateTokenXClaims().getFnr() } returns ansattFnr
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

                it("Should return TRENGER_OPPFOLGING when user has answered Ja") {
                    every { tokenValidator.validateTokenXClaims().getFnr() } returns ansattFnr
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

                it("Should return NO_RESPONSE when user hasn't answered") {
                    every { tokenValidator.validateTokenXClaims().getFnr() } returns ansattFnr
                    every { responseDao.find(any(), SEN_OPPFOLGING_V2, any()) } returns emptyList()
                    val status = controller.status()
                    status.responseStatus shouldBe NO_RESPONSE
                }

                it("Should return isPilot=true when user belongs to pilot") {
                    every { tokenValidator.validateTokenXClaims().getFnr() } returns ansattFnr
                    every { behandlendeEnhetClient.getBehandlendeEnhet(ansattFnr) } returns
                        BehandlendeEnhet(
                            "0331",
                            "Testkontor",
                        )
                    val status = controller.status()
                    status.responseStatus shouldBe NO_RESPONSE
                    status.isPilot shouldBe true
                }

                it("Should journalfore submitted answers for pilot") {
                    every { tokenValidator.validateTokenXClaims().getFnr() } returns ansattFnr
                    every { behandlendeEnhetClient.getBehandlendeEnhet(ansattFnr) } returns
                        BehandlendeEnhet(
                            "0314",
                            "Testkontor",
                        )
                    every { syfoopfpdfgenService.getSenOppfolgingReceiptPdf(any()) } returns ByteArray(1)
                    val responses =
                        listOf(
                            SenOppfolgingQuestionV2(BEHOV_FOR_OPPFOLGING, "Hei", "JA", "Ja"),
                        )
                    controller.submitForm(
                        SenOppfolgingDTOV2(
                            responses,
                        ),
                    )
                    verify(exactly = 1) {
                        responseDao.saveFormResponse(any(), any(), SEN_OPPFOLGING_V2, any(), any())
                    }

                    verify(exactly = 1) { dokarkivClient.postDocumentToDokarkiv(ansattFnr, any(), any()) }
                }
            }
        },
    )
