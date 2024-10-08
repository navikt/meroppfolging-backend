package no.nav.syfo.varsel

import com.github.tomakehurst.wiremock.WireMockServer
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.wiremock.ListenerMode
import io.kotest.extensions.wiremock.WireMockListener
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.syfo.LocalApplication
import no.nav.syfo.dokarkiv.DokarkivClient
import no.nav.syfo.pdl.stubHentPerson
import no.nav.syfo.syfoopppdfgen.PdfgenService
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
class VarselServiceTest : DescribeSpec() {

    @MockkBean(relaxed = true)
    lateinit var pdfgenService: PdfgenService

    @MockkBean(relaxed = true)
    lateinit var dokarkivClient: DokarkivClient

    @Autowired
    lateinit var varselService: VarselService

    @Autowired
    lateinit var sykepengedagerInformasjonDAO: SykepengedagerInformasjonDAO

    @Autowired
    lateinit var sykmeldingDao: SykmeldingDao

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    init {
        extension(SpringExtension)

        val pdlServer = WireMockServer(8080)
        listener(WireMockListener(pdlServer, ListenerMode.PER_TEST))

        beforeTest {
            pdlServer.stubHentPerson(yearsOld = 55)
            jdbcTemplate.execute("TRUNCATE TABLE UTSENDT_VARSEL CASCADE")
            jdbcTemplate.execute("TRUNCATE TABLE SYKMELDING CASCADE")
            jdbcTemplate.execute("TRUNCATE TABLE SYKEPENGEDAGER_INFORMASJON CASCADE")
        }

        describe("VarselService") {
            it("Should store utsendt varsel") {
                varselService.sendMerOppfolgingVarsel(
                    MerOppfolgingVarselDTO(
                        personIdent = "12345678910",
                        utbetalingId = "utbetalingId",
                        sykmeldingId = "sykmeldingId",
                    ),
                )

                val utsendtVarsel = varselService.getUtsendtVarsel("12345678910")

                utsendtVarsel shouldNotBe null
                utsendtVarsel!!.personIdent shouldBe "12345678910"
                utsendtVarsel.utbetalingId shouldBe "utbetalingId"
                utsendtVarsel.sykmeldingId shouldBe "sykmeldingId"
            }

            it("Should find mer oppfolging varsel to be sent") {
                // Should send varsel
                createMockdataForFnr(
                    fnr = "12345678910",
                    activeSykmelding = true,
                    gjenstaendeSykedager = "70",
                    forelopigBeregnetSlutt = LocalDate.now().plusDays(50),
                )

                // No varsel due to 100 gjenstående sykedager
                createMockdataForFnr(
                    fnr = "12345678911",
                    activeSykmelding = true,
                    gjenstaendeSykedager = "100",
                    forelopigBeregnetSlutt = LocalDate.now().plusDays(50),
                )

                // No varsel due to no active sykmelding
                createMockdataForFnr(
                    fnr = "12345678912",
                    activeSykmelding = false,
                    gjenstaendeSykedager = "70",
                    forelopigBeregnetSlutt = LocalDate.now().plusDays(50),
                )

                val merOppfolgingVarselToBeSent = varselService.findMerOppfolgingVarselToBeSent()

                merOppfolgingVarselToBeSent.size shouldBe 1
                merOppfolgingVarselToBeSent[0].personIdent shouldBe "12345678910"
            }
        }
    }

    private fun createMockdataForFnr(
        fnr: String,
        activeSykmelding: Boolean,
        gjenstaendeSykedager: String,
        forelopigBeregnetSlutt: LocalDate,
    ): String {
        sykmeldingDao.persistSykmelding(
            sykmeldingId = UUID.randomUUID().toString(),
            employeeIdentificationNumber = fnr,
            fom = LocalDate.now().minusDays(10),
            tom = if (activeSykmelding) LocalDate.now().plusDays(10) else LocalDate.now().minusDays(3),
        )

        sykepengedagerInformasjonDAO.persistSykepengedagerInformasjon(
            SykepengedagerInformasjonDTO(
                id = UUID.randomUUID().toString(),
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
