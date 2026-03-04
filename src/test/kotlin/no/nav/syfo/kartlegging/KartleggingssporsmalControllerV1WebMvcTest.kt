package no.nav.syfo.kartlegging

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.MockOAuth2ServerAutoConfiguration
import no.nav.syfo.kartlegging.domain.KandidatStatus
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartlegging.domain.PersistedKartleggingssporsmal
import no.nav.syfo.kartlegging.domain.formsnapshot.FormSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.FormSnapshotFieldOption
import no.nav.syfo.kartlegging.domain.formsnapshot.RadioGroupFieldSnapshot
import no.nav.syfo.kartlegging.service.KandidatService
import no.nav.syfo.kartlegging.service.KartleggingssporsmalService
import no.nav.syfo.metric.Metric
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MockMvcResultMatchersDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

@WebMvcTest(KartleggingssporsmalControllerV1::class)
@Import(MockOAuth2ServerAutoConfiguration::class)
@TestPropertySource(
    properties = [
        "BRO_FRONTEND_CLIENT_ID=bro-frontend-client-id",
        "no.nav.security.jwt.issuer.tokenx.discoveryurl=http://localhost:\${mock-oauth2-server.port}/tokenx/.well-known/openid-configuration",
        "no.nav.security.jwt.issuer.tokenx.accepted_audience=meroppfolging-backend-client-id",
    ],
)
@ApplyExtension(SpringExtension::class)
class KartleggingssporsmalControllerV1WebMvcTest : DescribeSpec() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var kartleggingssporsmalService: KartleggingssporsmalService

    @MockkBean
    private lateinit var kandidatService: KandidatService

    @MockkBean(relaxed = true)
    private lateinit var metric: Metric

    private val fnr = "12345678910"
    private val kandidatId = UUID.randomUUID()

    private fun bearerToken(): String = mockOAuth2Server.issueToken(
        "tokenx",
        "test-client",
        DefaultOAuth2TokenCallback(
            issuerId = "tokenx",
            subject = fnr,
            audience = listOf("meroppfolging-backend-client-id"),
            claims = mapOf(
                "pid" to fnr,
                "client_id" to "bro-frontend-client-id",
                "acr" to "Level4",
            ),
            expiry = 3600,
        ),
    ).serialize()

    private fun formSnapshot() = FormSnapshot(
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

    private fun MockMvcResultMatchersDsl.assertPersistedKartleggingssporsmalJson(prefix: String, uuid: UUID,) {
        jsonPath("$prefix.uuid") { value(uuid.toString()) }
        jsonPath("$prefix.fnr") { value(fnr) }
        jsonPath("$prefix.kandidatId") { value(kandidatId.toString()) }
        jsonPath("$prefix.createdAt") { exists() }
        jsonPath("$prefix.formSnapshot.formIdentifier") { value("kartlegging-test-form") }
        jsonPath("$prefix.formSnapshot.formSemanticVersion") { value("1.0.0") }
        jsonPath("$prefix.formSnapshot.formSnapshotVersion") { value("1") }
        jsonPath("$prefix.formSnapshot.fieldSnapshots") { isArray() }
        jsonPath("$prefix.formSnapshot.fieldSnapshots.length()") { value(3) }
        jsonPath("$prefix.formSnapshot.fieldSnapshots[0].fieldId") { value("hvorSannsynligTilbakeTilJobben") }
        jsonPath("$prefix.formSnapshot.fieldSnapshots[0].fieldType") { value("RADIO_GROUP") }
        jsonPath("$prefix.formSnapshot.fieldSnapshots[0].label") { value("Label 1") }
        jsonPath("$prefix.formSnapshot.fieldSnapshots[0].options") { isArray() }
        jsonPath("$prefix.formSnapshot.fieldSnapshots[0].options.length()") { value(2) }
        jsonPath("$prefix.formSnapshot.fieldSnapshots[0].options[0].optionId") { value("opt1") }
        jsonPath("$prefix.formSnapshot.fieldSnapshots[0].options[0].optionLabel") { value("Option 1") }
        jsonPath("$prefix.formSnapshot.fieldSnapshots[0].options[0].wasSelected") { value(true) }
        jsonPath("$prefix.formSnapshot.fieldSnapshots[0].options[1].optionId") { value("opt2") }
        jsonPath("$prefix.formSnapshot.fieldSnapshots[0].options[1].optionLabel") { value("Option 2") }
        jsonPath("$prefix.formSnapshot.fieldSnapshots[0].options[1].wasSelected") { value(false) }
    }

    init {
        beforeTest {
            every { kandidatService.getKandidatByFnr(fnr) } returns KartleggingssporsmalKandidat(
                kandidatId = kandidatId,
                personIdent = fnr,
                status = KandidatStatus.KANDIDAT,
                createdAt = Instant.now(),
            )
        }

        describe("POST /api/v1/kartleggingssporsmal") {
            it("returns all expected JSON properties") {
                val formSnapshot = formSnapshot()
                val uuid = UUID.randomUUID()
                val persisted = PersistedKartleggingssporsmal(
                    uuid = uuid,
                    fnr = fnr,
                    kandidatId = kandidatId,
                    formSnapshot = formSnapshot,
                    createdAt = Instant.now(),
                )

                every { kartleggingssporsmalService.validateFormSnapshot(any()) } just Runs
                every { kartleggingssporsmalService.persistAndPublishKartleggingssporsmal(any(), any()) } returns
                    persisted

                val requestBody = mapOf("formSnapshot" to formSnapshot)

                mockMvc.post("/api/v1/kartleggingssporsmal") {
                    header(HttpHeaders.AUTHORIZATION, "Bearer ${bearerToken()}")
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(requestBody)
                }.andExpect {
                    status { isOk() }
                    content { contentType(MediaType.APPLICATION_JSON) }
                    assertPersistedKartleggingssporsmalJson("$", uuid)
                }

                val formSnapshotSlot: CapturingSlot<FormSnapshot> = slot()
                verify(exactly = 1) {
                    kartleggingssporsmalService.validateFormSnapshot(capture(formSnapshotSlot))
                }
                formSnapshotSlot.captured shouldBe formSnapshot
            }
        }

        describe("GET /api/v1/kartleggingssporsmal/kandidat-status") {
            it("returns all expected JSON properties when kandidat with previous form response") {
                val formSnapshot = formSnapshot()
                val uuid = UUID.randomUUID()
                val persisted = PersistedKartleggingssporsmal(
                    uuid = uuid,
                    fnr = fnr,
                    kandidatId = kandidatId,
                    formSnapshot = formSnapshot,
                    createdAt = Instant.now(),
                )

                every { kartleggingssporsmalService.getLatestKartleggingssporsmal(kandidatId) } returns persisted

                mockMvc.get("/api/v1/kartleggingssporsmal/kandidat-status") {
                    header(HttpHeaders.AUTHORIZATION, "Bearer ${bearerToken()}")
                }.andExpect {
                    status { isOk() }
                    content { contentType(MediaType.APPLICATION_JSON) }
                    jsonPath("$.isKandidat") { value(true) }
                    assertPersistedKartleggingssporsmalJson("$.formResponse", uuid)
                }
            }

            it("returns isKandidat false with null formResponse when not kandidat") {
                every { kandidatService.getKandidatByFnr(fnr) } returns null

                mockMvc.get("/api/v1/kartleggingssporsmal/kandidat-status") {
                    header(HttpHeaders.AUTHORIZATION, "Bearer ${bearerToken()}")
                }.andExpect {
                    status { isOk() }
                    content { contentType(MediaType.APPLICATION_JSON) }
                    jsonPath("$.isKandidat") { value(false) }
                    jsonPath("$.formResponse") { value(null as Any?) }
                }
            }
        }
    }
}
