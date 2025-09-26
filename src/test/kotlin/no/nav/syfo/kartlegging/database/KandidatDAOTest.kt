package no.nav.syfo.kartlegging.database

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.syfo.LocalApplication
import no.nav.syfo.kartlegging.domain.KandidatStatus
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalKandidat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import java.time.Instant
import java.util.UUID

@EmbeddedKafka
@SpringBootTest(classes = [LocalApplication::class])
class KandidatDAOTest : DescribeSpec() {
    @Autowired
    private lateinit var kandidatDAO: KandidatDAO

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    init {
        extension(SpringExtension)

        beforeTest {
            jdbcTemplate.execute("TRUNCATE TABLE KARTLEGGINGSPORSMAL_KANDIDAT CASCADE")
        }

        describe("KandidatDAO.persistKandidat") {
            it("persists a kandidat and can retrieve it by fnr") {
                val fnr = "12345678910"
                val kandidatId = UUID.randomUUID()
                val createdAt = Instant.now()

                val kandidat = KartleggingssporsmalKandidat(
                    personIdent = fnr,
                    kandidatId = kandidatId,
                    status = KandidatStatus.KANDIDAT,
                    createdAt = createdAt,
                )
                kandidatDAO.persistKandidat(kandidat)

                val persisted = kandidatDAO.findKandidatByFnr(fnr)
                persisted shouldNotBe null
                persisted!!.personIdent shouldBe fnr
                persisted.kandidatId shouldBe kandidatId
                persisted.status shouldBe KandidatStatus.KANDIDAT
                // createdAt stored with timestamp precision; allow equality check
                persisted.createdAt.epochSecond shouldBe createdAt.epochSecond
                persisted.isKandidat() shouldBe true
            }

            it("persists a ikke kandidat and retrieves correct status and helper result") {
                val fnr = "32109876543"
                val kandidatId = UUID.randomUUID()
                val createdAt = Instant.now()

                val kandidat = KartleggingssporsmalKandidat(
                    personIdent = fnr,
                    kandidatId = kandidatId,
                    status = KandidatStatus.IKKE_KANDIDAT,
                    createdAt = createdAt,
                )
                kandidatDAO.persistKandidat(kandidat)

                val persisted = kandidatDAO.findKandidatByFnr(fnr)
                persisted shouldNotBe null
                persisted!!.status shouldBe KandidatStatus.IKKE_KANDIDAT
                persisted.isKandidat() shouldBe false
            }
        }

        describe("KandidatDAO.findKandidatByFnr") {
            it("returns null when no kandidat exists") {
                kandidatDAO.findKandidatByFnr("00000000000").shouldBeNull()
            }

            it("returns the latest kandidat by created_at when multiple exist for same fnr") {
                val fnr = "10987654321"
                val old = KartleggingssporsmalKandidat(
                    personIdent = fnr,
                    kandidatId = UUID.randomUUID(),
                    status = KandidatStatus.KANDIDAT,
                    createdAt = Instant.now().minusSeconds(3600),
                )
                val new = KartleggingssporsmalKandidat(
                    personIdent = fnr,
                    kandidatId = UUID.randomUUID(),
                    status = KandidatStatus.KANDIDAT,
                    createdAt = Instant.now(),
                )
                kandidatDAO.persistKandidat(old)
                kandidatDAO.persistKandidat(new)

                val latest = kandidatDAO.findKandidatByFnr(fnr)
                latest shouldNotBe null
                latest!!.kandidatId shouldBe new.kandidatId
                latest.createdAt.epochSecond shouldBe new.createdAt.epochSecond
            }
        }
    }
}
