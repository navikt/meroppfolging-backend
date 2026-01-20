package no.nav.syfo.kartlegging

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.auth.getFnr
import no.nav.syfo.dokarkiv.DokarkivClient
import no.nav.syfo.kartlegging.database.KandidatDAO
import no.nav.syfo.kartlegging.database.KartleggingssporsmalDAO
import no.nav.syfo.kartlegging.domain.KandidatStatus
import no.nav.syfo.kartlegging.domain.Kartleggingssporsmal
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalRequest
import no.nav.syfo.kartlegging.domain.formsnapshot.FormSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.FormSnapshotFieldOption
import no.nav.syfo.kartlegging.domain.formsnapshot.RadioGroupFieldSnapshot
import no.nav.syfo.kartlegging.exception.InvalidFormException
import no.nav.syfo.kartlegging.kafka.KartleggingssvarKafkaProducer
import no.nav.syfo.kartlegging.service.KandidatService
import no.nav.syfo.kartlegging.service.KartleggingssporsmalService
import no.nav.syfo.metric.Metric
import no.nav.syfo.syfoopppdfgen.PdfgenService
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID

class KartleggingssporsmalControllerV1Test :
    DescribeSpec({
        val tokenValidationContextHolder = mockk<TokenValidationContextHolder>(relaxed = true)
        val tokenValidator = mockk<TokenValidator>(relaxed = true)
        val kartleggingssporsmalDAO = mockk<KartleggingssporsmalDAO>(relaxed = true)
        val kandidatDAO = mockk<KandidatDAO>(relaxed = true)
        val kafkaProducer = mockk<KartleggingssvarKafkaProducer>(relaxed = true)
        val pdfgenService = mockk<PdfgenService>(relaxed = true)
        val dokarkivClient = mockk<DokarkivClient>(relaxed = true)
        val kartleggingssporsmalService = KartleggingssporsmalService(
            kartleggingssporsmalDAO,
            kafkaProducer,
            pdfgenService,
            dokarkivClient,
        )
        val kandidatService = KandidatService(kandidatDAO)
        val metric = mockk<Metric>(relaxed = true)

        val controller =
            KartleggingssporsmalControllerV1(
                broFrontendClientId = "broFrontendClientId",
                tokenValidationContextHolder = tokenValidationContextHolder,
                kartleggingssporsmalService = kartleggingssporsmalService,
                kandidatService = kandidatService,
                metric = metric,
            ).apply {
                this.tokenValidator = tokenValidator
            }

        val fnr = "12345678910"
        val kandidatId = UUID.randomUUID()

        beforeTest {
            clearAllMocks()
            every { tokenValidator.validateTokenXClaims().getFnr() } returns fnr
            every { kandidatDAO.findKandidatByFnr(fnr) } returns KartleggingssporsmalKandidat(
                kandidatId = kandidatId,
                personIdent = fnr,
                status = KandidatStatus.KANDIDAT,
                createdAt = Instant.now(),
            )
        }

        describe("POST /api/v1/kartleggingssporsmal") {
            it("returns 200 OK and persists dto matching request body") {
                val formSnapshot = FormSnapshot(
                    formIdentifier = "kartlegging-test-form",
                    formSemanticVersion = "1.0.0",
                    formSnapshotVersion = "1",
                    fieldSnapshots = listOf(
                        RadioGroupFieldSnapshot(
                            fieldId = "hvorSannsynligTilbakeTilJobben",
                            label = "Label 1",
                            options = listOf(
                                FormSnapshotFieldOption("opt1", "Option 1", wasSelected = true),
                                FormSnapshotFieldOption("opt2", "Option 2", wasSelected = false),
                            ),
                        ),
                        RadioGroupFieldSnapshot(
                            fieldId = "samarbeidOgRelasjonTilArbeidsgiver",
                            label = "Label 2",
                            options = listOf(
                                FormSnapshotFieldOption("opt1", "Option 1", wasSelected = true),
                                FormSnapshotFieldOption("opt2", "Option 2", wasSelected = false),
                            ),
                        ),
                        RadioGroupFieldSnapshot(
                            fieldId = "naarTilbakeTilJobben",
                            label = "Label 3",
                            options = listOf(
                                FormSnapshotFieldOption("opt1", "Option 1", wasSelected = true),
                                FormSnapshotFieldOption("opt2", "Option 2", wasSelected = false),
                            ),
                        ),
                    ),
                )
                val request = KartleggingssporsmalRequest(formSnapshot = formSnapshot)

                val response = controller.postKartleggingssporsmal(request)

                response.statusCode shouldBe HttpStatus.OK

                val persistedSlot: CapturingSlot<Kartleggingssporsmal> = slot()
                verify(exactly = 1) {
                    kartleggingssporsmalDAO.persistKartleggingssporsmal(capture(persistedSlot), any())
                }
                verify(exactly = 1) { kafkaProducer.publishResponse(any()) }

                persistedSlot.captured.fnr shouldBe fnr
                persistedSlot.captured.formSnapshot shouldBe formSnapshot
            }

            it("returns 400 InvalidFormException if form snapshot is missing required fieldId") {
                val formSnapshot = FormSnapshot(
                    formIdentifier = "kartlegging-test-form",
                    formSemanticVersion = "1.0.0",
                    formSnapshotVersion = "1",
                    fieldSnapshots = listOf(
                        RadioGroupFieldSnapshot(
                            fieldId = "hvorSannsynligTilbakeTilJobben",
                            label = "Label 1",
                            options = listOf(
                                FormSnapshotFieldOption("opt1", "Option 1", wasSelected = true),
                                FormSnapshotFieldOption("opt2", "Option 2", wasSelected = false),
                            ),
                        ),
                        RadioGroupFieldSnapshot(
                            fieldId = "samarbeidOgRelasjonTilArbeidsgiver",
                            label = "Label 2",
                            options = listOf(
                                FormSnapshotFieldOption("opt1", "Option 1", wasSelected = true),
                                FormSnapshotFieldOption("opt2", "Option 2", wasSelected = false),
                            ),
                        ),
                        // Missing required naarTilbakeTilJobben
                    ),
                )
                val request = KartleggingssporsmalRequest(formSnapshot = formSnapshot)

                shouldThrow<InvalidFormException> {
                    controller.postKartleggingssporsmal(request)
                }
            }

            it(
                "returns 400 InvalidFormException if form contains required RadioGroupFieldSnapshot with no option selected"
            ) {
                val formSnapshot = FormSnapshot(
                    formIdentifier = "kartlegging-test-form",
                    formSemanticVersion = "1.0.0",
                    formSnapshotVersion = "1",
                    fieldSnapshots = listOf(
                        RadioGroupFieldSnapshot(
                            fieldId = "hvorSannsynligTilbakeTilJobben",
                            label = "Label 1",
                            options = listOf(
                                FormSnapshotFieldOption("opt1", "Option 1", wasSelected = true),
                                FormSnapshotFieldOption("opt2", "Option 2", wasSelected = false),
                            ),
                        ),
                        RadioGroupFieldSnapshot(
                            fieldId = "samarbeidOgRelasjonTilArbeidsgiver",
                            label = "Label 2",
                            options = listOf(
                                FormSnapshotFieldOption("opt1", "Option 1", wasSelected = true),
                                FormSnapshotFieldOption("opt2", "Option 2", wasSelected = false),
                            ),
                        ),
                        RadioGroupFieldSnapshot(
                            fieldId = "naarTilbakeTilJobben",
                            label = "Label 3",
                            options = listOf(
                                // No option selected
                                FormSnapshotFieldOption("opt1", "Option 1", wasSelected = false),
                                FormSnapshotFieldOption("opt2", "Option 2", wasSelected = false),
                            ),
                        ),
                    ),
                )
                val request = KartleggingssporsmalRequest(formSnapshot = formSnapshot)

                shouldThrow<InvalidFormException> {
                    controller.postKartleggingssporsmal(request)
                }
            }
        }
    })
