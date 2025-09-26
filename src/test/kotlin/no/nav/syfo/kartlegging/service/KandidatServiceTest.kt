package no.nav.syfo.kartlegging.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import no.nav.syfo.LocalApplication
import no.nav.syfo.kartlegging.database.KandidatDAO
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartlegging.domain.KandidatStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import java.time.Instant
import java.util.UUID

@EmbeddedKafka
@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
class KandidatServiceTest : DescribeSpec() {
    @Autowired
    private lateinit var kandidatService: KandidatService

    @Autowired
    private lateinit var kandidatDAO: KandidatDAO

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    init {
        extension(SpringExtension)

        beforeTest {
            jdbcTemplate.execute("TRUNCATE TABLE KARTLEGGINGSPORSMAL_KANDIDAT CASCADE")
        }

        describe("KandidatService.persistKandidater") {
            it("rolls back all inserts when exception is thrown (partial failure)") {
                // Arrange: Two kandidater with the same kandidatId (primary key) so second insert fails
                val sharedId = UUID.randomUUID()
                val firstFnr = "11111111111"
                val secondFnr = "22222222222"
                val first = KartleggingssporsmalKandidat(
                    personIdent = firstFnr,
                    kandidatId = sharedId,
                    status = KandidatStatus.KANDIDAT,
                    createdAt = Instant.now()
                )
                val second = KartleggingssporsmalKandidat(
                    personIdent = secondFnr,
                    kandidatId = sharedId, // duplicate primary key -> will fail
                    status = KandidatStatus.KANDIDAT,
                    createdAt = Instant.now()
                )

                // Act
                shouldThrow<Exception> {
                    kandidatService.persistKandidater(listOf(first, second))
                }

                // Verify rollback: Table must be empty (no partial commit)
                val count: Int = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM KARTLEGGINGSPORSMAL_KANDIDAT", Int::class.java
                )!!
                count shouldBe 0

                kandidatDAO.findKandidatByFnr(firstFnr) shouldBe null
                kandidatDAO.findKandidatByFnr(secondFnr) shouldBe null
            }
        }

        it("persists 2 kandidater correctly") {
            // Arrange
            val firstKandidatId = UUID.randomUUID()
            val firstFnr = "11111111111"
            val secondKandidatId = UUID.randomUUID()
            val secondFnr = "22222222222"

            val first = KartleggingssporsmalKandidat(
                personIdent = firstFnr,
                kandidatId = firstKandidatId,
                status = KandidatStatus.KANDIDAT,
                createdAt = Instant.now()
            )
            val second = KartleggingssporsmalKandidat(
                personIdent = secondFnr,
                kandidatId = secondKandidatId,
                status = KandidatStatus.KANDIDAT,
                createdAt = Instant.now()
            )

            // Act
            kandidatService.persistKandidater(listOf(first, second))

            val count: Int = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM KARTLEGGINGSPORSMAL_KANDIDAT", Int::class.java
            )!!
            // Assert
            count shouldBe 2
        }
    }
}
