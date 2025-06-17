package no.nav.syfo.sykepengedagerinformasjon.database

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.BEFORE_CLASS
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
@AutoConfigureEmbeddedDatabase(provider = ZONKY, refresh = BEFORE_CLASS)
@EmbeddedKafka
@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
class SykepengedagerInformasjonDAOTest : DescribeSpec() {
    val personIdent = "12345678910"
    val utbetalingId = "123"
    val utbetalingId2 = "2123"

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
            utbetaling?.utbetalingCreatedAt?.toLocalDate() shouldBe createdAt.toLocalDate()
            utbetaling?.receivedAt?.toLocalDate() shouldBe expectedReceivedDate
        }

        it("Should return utbetaling with newest utbetaltTOM") {
            val expextedUtbetaltTOM = LocalDate.now().plusDays(33)

            sykepengedagerInformasjonDAO.persistSykepengedagerInformasjon(
                SykepengedagerInformasjonDTO(
                    id = utbetalingId,
                    personIdent = personIdent,
                    forelopigBeregnetSlutt = LocalDate.now().plusDays(24),
                    utbetaltTom = LocalDate.now().plusDays(32),
                    gjenstaendeSykedager = "24",
                    createdAt = LocalDateTime.now().minusDays(1),
                )
            )
            sykepengedagerInformasjonDAO.persistSykepengedagerInformasjon(
                SykepengedagerInformasjonDTO(
                    id = utbetalingId2,
                    personIdent = personIdent,
                    forelopigBeregnetSlutt = LocalDate.now().plusDays(24),
                    utbetaltTom = LocalDate.now().plusDays(33),
                    gjenstaendeSykedager = "24",
                    createdAt = LocalDateTime.now().minusDays(2),
                )
            )

            val utbetaling = sykepengedagerInformasjonDAO.fetchSykepengedagerInformasjonByIdent(
                personIdent = personIdent
            )

            utbetaling shouldNotBe null

            utbetaling?.utbetalingId shouldBe utbetalingId2
            utbetaling?.personIdent shouldBe personIdent
            utbetaling?.utbetalingCreatedAt?.toLocalDate() shouldBe LocalDate.now().minusDays(2)
            utbetaling?.utbetaltTom shouldBe expextedUtbetaltTOM
        }

        it("Should return utbetaling with newest createdAt when same utebetaltTOM") {
            val createdAtOldDate = LocalDateTime.now().minusDays(2)
            val expectedCreatedAtNewDate = LocalDateTime.now().minusDays(1)

            sykepengedagerInformasjonDAO.persistSykepengedagerInformasjon(
                SykepengedagerInformasjonDTO(
                    id = utbetalingId,
                    personIdent = personIdent,
                    forelopigBeregnetSlutt = LocalDate.now().plusDays(24),
                    utbetaltTom = LocalDate.now().plusDays(33),
                    gjenstaendeSykedager = "24",
                    createdAt = expectedCreatedAtNewDate,
                )
            )
            sykepengedagerInformasjonDAO.persistSykepengedagerInformasjon(
                SykepengedagerInformasjonDTO(
                    id = utbetalingId2,
                    personIdent = personIdent,
                    forelopigBeregnetSlutt = LocalDate.now().plusDays(24),
                    utbetaltTom = LocalDate.now().plusDays(33),
                    gjenstaendeSykedager = "24",
                    createdAt = createdAtOldDate,
                )
            )

            val utbetaling = sykepengedagerInformasjonDAO.fetchSykepengedagerInformasjonByIdent(
                personIdent = personIdent
            )

            utbetaling shouldNotBe null

            utbetaling?.utbetalingId shouldBe utbetalingId
            utbetaling?.personIdent shouldBe personIdent
            utbetaling?.utbetalingCreatedAt?.toLocalDate() shouldBe expectedCreatedAtNewDate.toLocalDate()
            utbetaling?.utbetaltTom shouldBe LocalDate.now().plusDays(33)
        }
    }
}
