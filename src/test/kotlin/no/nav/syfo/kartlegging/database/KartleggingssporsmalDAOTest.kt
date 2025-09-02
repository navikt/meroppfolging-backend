package no.nav.syfo.kartlegging.database

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.syfo.LocalApplication
import no.nav.syfo.kartlegging.domain.Kartleggingssporsmal
import no.nav.syfo.kartlegging.domain.formsnapshot.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.test.context.EmbeddedKafka

@EmbeddedKafka
@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
class KartleggingssporsmalDAOTest : DescribeSpec() {
    @Autowired
    private lateinit var kartleggingssporsmalDAO: KartleggingssporsmalDAO

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    init {
        extension(SpringExtension)

        beforeTest {
            jdbcTemplate.execute("TRUNCATE TABLE KARTLEGGINGSPORSMAL CASCADE")
        }

        describe("KartleggingssporsmalDAO.persistKartleggingssporsmal") {
            it("persists a kartleggingssporsmal with correct JSON and created_at") {
                val fnr = "12345678910"
                val snapshot = FormSnapshot(
                    formIdentifier = "kartlegging_skjematype",
                    formSemanticVersion = "1.0.0",
                    formSnapshotVersion = "1",
                    fieldSnapshots = listOf(
                        TextFieldSnapshot(
                            fieldId = "tekst1",
                            label = "Hva er svaret?",
                            value = "Dette er et svar"
                        ),
                        SingleCheckboxFieldSnapshot(
                            fieldId = "enkel_checkbox",
                            label = "Har du lest og forstått?",
                            value = true
                        ),
                        CheckboxFieldSnapshot(
                            fieldId = "flervalg",
                            label = "Velg det som gjelder",
                            options = listOf(
                                FormSnapshotFieldOption("opt1", "Alternativ 1", wasSelected = true),
                                FormSnapshotFieldOption("opt2", "Alternativ 2", wasSelected = false)
                            )
                        ),
                        RadioGroupFieldSnapshot(
                            fieldId = "radio",
                            label = "Velg ett",
                            options = listOf(
                                FormSnapshotFieldOption("r1", "R1", wasSelected = false),
                                FormSnapshotFieldOption("r2", "R2", wasSelected = true)
                            )
                        )
                    ),
                    sections = listOf(
                        FormSection(
                            sectionId = "seksjon1",
                            sectionTitle = "Seksjon 1"
                        )
                    )
                )

                kartleggingssporsmalDAO.persistKartleggingssporsmal(
                    Kartleggingssporsmal(
                        fnr = fnr,
                        formSnapshot = snapshot
                    )
                )

                val persisted = kartleggingssporsmalDAO.getLatestKartleggingssporsmalerByFnr(fnr)
                persisted!!.fnr shouldBe fnr
                persisted.formSnapshot shouldBe snapshot
                persisted.uuid shouldNotBe null
                persisted.createdAt shouldNotBe null
            }
        }
    }
}
