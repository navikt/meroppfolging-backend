package no.nav.syfo.sykepengedagerinformasjon.database

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.syfo.LocalApplication
import no.nav.syfo.sykepengedagerinformasjon.domain.PSykepengedagerInformasjon
import no.nav.syfo.sykepengedagerinformasjon.domain.SykepengedagerInformasjonDTO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import java.time.LocalDate
import java.time.LocalDateTime

@EmbeddedKafka
@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
class SykepengedagerInformasjonDAOTest : DescribeSpec() {
    val personIdent = "12345678910"
    val utbetalingId = "123"

    @Autowired
    private lateinit var sykepengedagerInformasjonDAO: SykepengedagerInformasjonDAO

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    init {
        extension(SpringExtension)

        beforeTest {
            jdbcTemplate.execute("TRUNCATE TABLE SYKEPENGEDAGER_INFORMASJON CASCADE")
        }

        it("Should persist received utbetaling record") {
            val createdAt = LocalDateTime.now().minusDays(1)
            val expectedReceivedDate = LocalDateTime.now().toLocalDate()

            sykepengedagerInformasjonDAO.persistSykepengedagerInformasjon(
                SykepengedagerInformasjonDTO(
                    id = utbetalingId,
                    personIdent = personIdent,
                    forelopigBeregnetSlutt = LocalDate.now().plusDays(24),
                    utbetaltTom = LocalDate.now().plusDays(33),
                    gjenstaendeSykedager = "33",
                    createdAt = createdAt,
                )
            )

            val utbetaling: PSykepengedagerInformasjon? = sykepengedagerInformasjonDAO.fetchSykepengedagerInformasjon(
                utbetalingId = utbetalingId,
            )

            utbetaling shouldNotBe null

            utbetaling?.utbetalingId shouldBe utbetalingId
            utbetaling?.personIdent shouldBe personIdent
            utbetaling?.utbetalingCreatedAt shouldBe createdAt
            utbetaling?.receivedAt?.toLocalDate() shouldBe expectedReceivedDate
        }
    }
}
