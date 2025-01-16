package no.nav.syfo.varsel

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import no.nav.syfo.LocalApplication
import no.nav.syfo.sykepengedagerinformasjon.database.SykepengedagerInformasjonDAO
import no.nav.syfo.sykepengedagerinformasjon.domain.SykepengedagerInformasjonDTO
import no.nav.syfo.sykmelding.database.SykmeldingDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@EmbeddedKafka
@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
class VarselRepositoryTest : DescribeSpec() {
    @Autowired
    private lateinit var varselRepository: VarselRepository

    @Autowired
    private lateinit var sykepengedagerInformasjonDAO: SykepengedagerInformasjonDAO

    @Autowired
    lateinit var sykmeldingDao: SykmeldingDao

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    init {
        extension(SpringExtension)

        beforeTest {
            jdbcTemplate.execute("TRUNCATE TABLE UTSENDT_VARSEL CASCADE")
            jdbcTemplate.execute("TRUNCATE TABLE COPY_UTSENDT_VARSEL_ESYFOVARSEL CASCADE")
            jdbcTemplate.execute("TRUNCATE TABLE SYKMELDING CASCADE")
            jdbcTemplate.execute("TRUNCATE TABLE sykepengedager_informasjon CASCADE")
        }

        val personIdent = "12345678910"
        val utbetalingId = "123"
        val sykmeldingId = "321"
        val sykmeldingId2 = "321-2"

        describe("VarselRepository") {
            it("Should return 1 varsel") {
                createMockdataForFnr(
                    fnr = "12345678910",
                    sykmeldingId = sykmeldingId,
                    utbetalingId = utbetalingId,
                    activeSykmelding = true,
                    gjenstaendeSykedager = "70",
                    forelopigBeregnetSlutt = LocalDate.now().plusDays(50),
                )
                val candidates = varselRepository.fetchMerOppfolgingVarselToBeSent()

                candidates.size shouldBe 1
            }

            it("Should return 0 varsel if there are several utbetalingIds for the same person with sent varsel") {
                createMockdataForFnr(
                    fnr = "12345678910",
                    sykmeldingId = sykmeldingId,
                    utbetalingId = utbetalingId,
                    activeSykmelding = true,
                    gjenstaendeSykedager = "70",
                    forelopigBeregnetSlutt = LocalDate.now().plusDays(50),
                )

                createMockdataForFnr(
                    fnr = "12345678910",
                    sykmeldingId = sykmeldingId,
                    utbetalingId = "456",
                    activeSykmelding = true,
                    gjenstaendeSykedager = "70",
                    forelopigBeregnetSlutt = LocalDate.now().plusDays(50),
                )

                varselRepository.storeUtsendtVarsel(personIdent, utbetalingId, sykmeldingId)

                val candidates = varselRepository.fetchMerOppfolgingVarselToBeSent()

                candidates.size shouldBe 0
            }

            it("Should return 1 varsel if there are several sykmeldinger for the same person") {
                createMockdataForFnr(
                    fnr = "12345678910",
                    sykmeldingId = sykmeldingId,
                    utbetalingId = utbetalingId,
                    activeSykmelding = true,
                    gjenstaendeSykedager = "70",
                    forelopigBeregnetSlutt = LocalDate.now().plusDays(50),
                )

                createMockdataForFnr(
                    fnr = "12345678910",
                    sykmeldingId = sykmeldingId2,
                    utbetalingId = "456",
                    activeSykmelding = true,
                    gjenstaendeSykedager = "70",
                    forelopigBeregnetSlutt = LocalDate.now().plusDays(50),
                )

                val candidates = varselRepository.fetchMerOppfolgingVarselToBeSent()
                candidates.size shouldBe 1
                candidates[0].sykmeldingId shouldBe sykmeldingId2
            }
        }
    }

    private fun createMockdataForFnr(
        fnr: String,
        sykmeldingId: String,
        utbetalingId: String,
        activeSykmelding: Boolean,
        gjenstaendeSykedager: String,
        forelopigBeregnetSlutt: LocalDate,
    ): String {
        sykmeldingDao.persistSykmelding(
            sykmeldingId = sykmeldingId,
            employeeIdentificationNumber = fnr,
            fom = LocalDate.now().minusDays(10),
            tom = if (activeSykmelding) LocalDate.now().plusDays(10) else LocalDate.now().minusDays(3),
        )

        sykepengedagerInformasjonDAO.persistSykepengedagerInformasjon(
            SykepengedagerInformasjonDTO(
                id = utbetalingId,
                personIdent = fnr,
                forelopigBeregnetSlutt = forelopigBeregnetSlutt,
                utbetaltTom = LocalDate.now().plusDays(10),
                gjenstaendeSykedager = gjenstaendeSykedager,
                createdAt = LocalDateTime.now(),
            ),
        )

        return fnr
    }
}
