package no.nav.syfo.kartlegging

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.NoAccess
import no.nav.syfo.kartlegging.domain.PersistedKartleggingssporsmal
import no.nav.syfo.kartlegging.domain.formsnapshot.FormSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.FormSnapshotFieldOption
import no.nav.syfo.kartlegging.domain.formsnapshot.RadioGroupFieldSnapshot
import no.nav.syfo.kartlegging.exception.KandidatNotFoundException
import no.nav.syfo.kartlegging.exception.UserResponseNotFoundException
import no.nav.syfo.kartlegging.service.KandidatService
import no.nav.syfo.kartlegging.service.KartleggingssporsmalService
import no.nav.syfo.veiledertilgang.VeilederTilgangClient
import org.springframework.http.HttpStatusCode
import java.time.Instant
import java.util.UUID

class KartleggingssporsmalVeilederControllerV1Test :
    DescribeSpec({
        val tokenValidationContextHolder = mockk<TokenValidationContextHolder>(relaxed = true)
        val veilederTilgangClient = mockk<VeilederTilgangClient>(relaxed = true)
        val kartleggingssporsmalService = mockk<KartleggingssporsmalService>(relaxed = true)
        val kandidatService = mockk<KandidatService>(relaxed = true)

        val controller = KartleggingssporsmalVeilederControllerV1(
            tokenValidationContextHolder = tokenValidationContextHolder,
            veilederTilgangClient = veilederTilgangClient,
            kartleggingssporsmalService = kartleggingssporsmalService,
            kandidatService = kandidatService
        )

        beforeTest { clearAllMocks() }

        describe("GET /api/v1/internad/kartleggingssporsmal/{uuid}") {
            it("returns 200 OK with persisted kartleggingssporsmal when veileder has access") {
                val uuid = UUID.randomUUID()
                val fnr = "12345678910"
                val kandidatId = UUID.randomUUID()

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
                val persisted = PersistedKartleggingssporsmal(
                    uuid = uuid,
                    fnr = fnr,
                    kandidatId = kandidatId,
                    formSnapshot = formSnapshot,
                    createdAt = Instant.now(),
                )

                every { kartleggingssporsmalService.getKartleggingssporsmalByUuid(uuid) } returns persisted
                every { veilederTilgangClient.hasVeilederTilgangToPerson(any(), any()) } returns true

                val response = controller.getKartleggingssporsmal(uuid)

                response.statusCode.isSameCodeAs(HttpStatusCode.valueOf(200)) shouldBe true
                val body = response.body!!
                body.uuid shouldBe uuid
                body.fnr shouldBe fnr
                body.formSnapshot shouldBe formSnapshot
            }

            it("throws UserResponseNotFoundException when uuid not found") {
                val uuid = UUID.randomUUID()
                every { kartleggingssporsmalService.getKartleggingssporsmalByUuid(uuid) } returns null

                shouldThrow<UserResponseNotFoundException> { controller.getKartleggingssporsmal(uuid) }
            }

            it("throws NoAccess when veileder does not have access") {
                val uuid = UUID.randomUUID()
                val fnr = "12345678910"
                val kandidatId = UUID.randomUUID()

                val persisted = PersistedKartleggingssporsmal(
                    uuid = uuid,
                    fnr = fnr,
                    kandidatId = kandidatId,
                    formSnapshot = FormSnapshot(
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
                    ),
                    createdAt = Instant.now(),
                )
                every { kartleggingssporsmalService.getKartleggingssporsmalByUuid(uuid) } returns persisted
                every { veilederTilgangClient.hasVeilederTilgangToPerson(any(), any()) } returns false

                shouldThrow<NoAccess> { controller.getKartleggingssporsmal(uuid) }
            }
        }

        describe("GET /api/v1/internad/kartleggingssporsmal/{kandidatId}/svar") {
            it("should throw KandidatNotFoundException when kandidat not found") {
                val kandidatId = UUID.randomUUID()

                every { kandidatService.getKandidatByKandidatId(kandidatId) } returns null

                shouldThrow<KandidatNotFoundException> {
                    controller.getKartleggingssporsmalSvar(kandidatId)
                }
            }

            it("should throw NoAccess when veileder does not have access") {
                val kandidatId = UUID.randomUUID()
                val fnr = "12345678910"

                every { kandidatService.getKandidatByKandidatId(kandidatId) } returns
                    no.nav.syfo.kartlegging.domain.KartleggingssporsmalKandidat(
                        personIdent = fnr,
                        kandidatId = kandidatId,
                        status = no.nav.syfo.kartlegging.domain.KandidatStatus.KANDIDAT,
                        createdAt = Instant.now(),
                    )
                every { veilederTilgangClient.hasVeilederTilgangToPerson(any(), any()) } returns false

                shouldThrow<NoAccess> {
                    controller.getKartleggingssporsmalSvar(kandidatId)
                }
            }

            it("should return 200 OK with persisted kartleggingssporsmal for kandidatId") {
                val kandidatId = UUID.randomUUID()
                val fnr = "12345678910"

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
                val persisted = PersistedKartleggingssporsmal(
                    uuid = UUID.randomUUID(),
                    fnr = fnr,
                    kandidatId = kandidatId,
                    formSnapshot = formSnapshot,
                    createdAt = Instant.now(),
                )

                every { kandidatService.getKandidatByKandidatId(kandidatId) } returns
                    no.nav.syfo.kartlegging.domain.KartleggingssporsmalKandidat(
                        personIdent = fnr,
                        kandidatId = kandidatId,
                        status = no.nav.syfo.kartlegging.domain.KandidatStatus.KANDIDAT,
                        createdAt = Instant.now(),
                    )
                every { veilederTilgangClient.hasVeilederTilgangToPerson(any(), any()) } returns true
                every { kartleggingssporsmalService.getLatestKartleggingssporsmal(kandidatId) } returns persisted

                val response = controller.getKartleggingssporsmalSvar(kandidatId)

                response.statusCode.isSameCodeAs(HttpStatusCode.valueOf(200)) shouldBe true
                val body = response.body!!
                body.uuid shouldBe persisted.uuid
                body.formSnapshot shouldBe formSnapshot
            }
        }
    })
