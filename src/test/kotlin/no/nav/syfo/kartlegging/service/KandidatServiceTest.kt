package no.nav.syfo.kartlegging.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import no.nav.syfo.LocalApplication
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
    private lateinit var jdbcTemplate: JdbcTemplate

    init {
        extension(SpringExtension)

        beforeTest {
            jdbcTemplate.execute("TRUNCATE TABLE KARTLEGGINGSPORSMAL_KANDIDAT CASCADE")
        }

        describe("KandidatService.persistKandidater") {
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
}
