package no.nav.syfo.sykmelding.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import no.nav.syfo.LocalApplication
import no.nav.syfo.sykmelding.domain.Periode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import java.time.LocalDate

@EmbeddedKafka
@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
class SykmeldingServiceTest : DescribeSpec() {

    val employeeIdentificationNumber = "12345678910"

    @Autowired
    private lateinit var sykmeldingService: SykmeldingService

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    init {
        extension(SpringExtension)

        beforeTest {
            jdbcTemplate.execute("TRUNCATE TABLE SYKMELDINGSPERIODE CASCADE")
        }

        it("Should only persist ongoing sykmeldingsperioder") {
            sykmeldingService.persistSykmeldingsperioder(
                "123",
                employeeIdentificationNumber,
                listOf(
                    Periode(
                        fom = LocalDate.now().minusDays(5),
                        tom = LocalDate.now().plusDays(5),
                    ),
                    Periode(
                        fom = LocalDate.now().minusDays(15),
                        tom = LocalDate.now().minusDays(6),
                    ),
                ),
                true,
            )

            val sykmeldingsperioder = sykmeldingService.getSykmeldingsperioder(
                employeeIdentificationNumber,
            )

            sykmeldingsperioder.size shouldBe 1
            sykmeldingsperioder[0].fom shouldBe LocalDate.now().minusDays(5)
            sykmeldingsperioder[0].tom shouldBe LocalDate.now().plusDays(5)
            sykmeldingsperioder[0].hasEmployer shouldBe true
            sykmeldingsperioder[0].sykmeldingId shouldBe "123"
        }

        it("Should delete tombstone records") {
            sykmeldingService.persistSykmeldingsperioder(
                "123",
                employeeIdentificationNumber,
                listOf(
                    Periode(
                        fom = LocalDate.now().minusDays(5),
                        tom = LocalDate.now().plusDays(5),
                    ),
                    Periode(
                        fom = LocalDate.now().minusDays(15),
                        tom = LocalDate.now().minusDays(6),
                    ),
                ),
                true,
            )

            val sykmeldingsperioder = sykmeldingService.getSykmeldingsperioder(
                employeeIdentificationNumber,
            )

            sykmeldingsperioder.size shouldBe 1

            sykmeldingService.deleteSykmeldingsperioder("123")
            val sykmeldingsperioderAfterDelete = sykmeldingService.getSykmeldingsperioder(
                employeeIdentificationNumber,
            )

            sykmeldingsperioderAfterDelete.size shouldBe 0
        }

        it("Should ignore duplicate sykmeldingsperioder") {
            sykmeldingService.persistSykmeldingsperioder(
                "123",
                employeeIdentificationNumber,
                listOf(
                    Periode(
                        fom = LocalDate.now().minusDays(5),
                        tom = LocalDate.now().plusDays(5),
                    ),
                    Periode(
                        fom = LocalDate.now().minusDays(15),
                        tom = LocalDate.now().minusDays(6),
                    ),
                ),
                true,
            )

            sykmeldingService.persistSykmeldingsperioder(
                "123",
                employeeIdentificationNumber,
                listOf(
                    Periode(
                        fom = LocalDate.now().minusDays(5),
                        tom = LocalDate.now().plusDays(5),
                    ),
                    Periode(
                        fom = LocalDate.now().minusDays(15),
                        tom = LocalDate.now().minusDays(6),
                    ),
                ),
                true,
            )

            val sykmeldingsperioder = sykmeldingService.getSykmeldingsperioder(
                employeeIdentificationNumber,
            )

            sykmeldingsperioder.size shouldBe 1
            sykmeldingsperioder[0].fom shouldBe LocalDate.now().minusDays(5)
            sykmeldingsperioder[0].tom shouldBe LocalDate.now().plusDays(5)
            sykmeldingsperioder[0].hasEmployer shouldBe true
            sykmeldingsperioder[0].sykmeldingId shouldBe "123"
        }
    }
}
