package no.nav.syfo.sykmelding.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
            jdbcTemplate.execute("TRUNCATE TABLE SYKMELDING CASCADE")
        }

        it("Should persist the earliest and latest date of a sykmelding") {
            sykmeldingService.persistSykmelding(
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
            )

            val sykmelding = sykmeldingService.getSykmelding(
                employeeIdentificationNumber,
            )

            sykmelding shouldNotBe null
            sykmelding!!.fom shouldBe LocalDate.now().minusDays(15)
            sykmelding.tom shouldBe LocalDate.now().plusDays(5)
            sykmelding.sykmeldingId shouldBe "123"
        }

        it("Should delete tombstone records") {
            sykmeldingService.persistSykmelding(
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
            )

            val sykmelding = sykmeldingService.getSykmelding(
                employeeIdentificationNumber,
            )

            sykmelding shouldNotBe null

            sykmeldingService.deleteSykmelding("123")
            val sykmeldingAfterDelete = sykmeldingService.getSykmelding(
                employeeIdentificationNumber,
            )

            sykmeldingAfterDelete shouldBe null
        }

        it("Should update already stored sykmeldinger") {
            val firstActiveSykmeldingsperiodeFom = LocalDate.now().minusDays(5)
            val firstActiveSykmeldingsperiodeTom = LocalDate.now().plusDays(5)
            val updatedSykmeldingFom = LocalDate.now().minusDays(4)
            val updatedSykmeldingTom = LocalDate.now().plusDays(12)

            sykmeldingService.persistSykmelding(
                "123",
                employeeIdentificationNumber,
                listOf(
                    Periode(
                        fom = firstActiveSykmeldingsperiodeFom,
                        tom = firstActiveSykmeldingsperiodeTom,
                    ),
                ),
            )

            val storedSykmelding = sykmeldingService.getSykmelding(
                employeeIdentificationNumber,
            )

            storedSykmelding shouldNotBe null
            storedSykmelding!!.sykmeldingId shouldBe "123"
            storedSykmelding.fom shouldBe firstActiveSykmeldingsperiodeFom
            storedSykmelding.tom shouldBe firstActiveSykmeldingsperiodeTom

            sykmeldingService.persistSykmelding(
                "123",
                employeeIdentificationNumber,
                listOf(
                    Periode(
                        fom = updatedSykmeldingFom,
                        tom = updatedSykmeldingTom,
                    ),
                ),
            )

            val updatedSykmelding = sykmeldingService.getSykmelding(
                employeeIdentificationNumber,
            )

            updatedSykmelding shouldNotBe null
            updatedSykmelding!!.fom shouldBe updatedSykmeldingFom
            updatedSykmelding.tom shouldBe updatedSykmeldingTom
            updatedSykmelding.sykmeldingId shouldBe "123"
        }
    }
}
