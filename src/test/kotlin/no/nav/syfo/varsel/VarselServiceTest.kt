package no.nav.syfo.varsel

import com.github.tomakehurst.wiremock.WireMockServer
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.wiremock.ListenerMode
import io.kotest.extensions.wiremock.WireMockListener
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.verify
import no.nav.syfo.LocalApplication
import no.nav.syfo.dkif.DkifClient
import no.nav.syfo.dokarkiv.DokarkivClient
import no.nav.syfo.dokarkiv.domain.DokarkivResponse
import no.nav.syfo.pdl.PdlClient
import no.nav.syfo.pdl.stubHentPerson
import no.nav.syfo.pdl.stubHentPersonError
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

    @SpykBean
    lateinit var pdlClient: PdlClient

    @MockkBean(relaxed = true)
    lateinit var dkifClient: DkifClient

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
            jdbcTemplate.execute("TRUNCATE TABLE COPY_UTSENDT_VARSEL_ESYFOVARSEL CASCADE")
            jdbcTemplate.execute("TRUNCATE TABLE SKIP_VARSELUTSENDING CASCADE")
        }

        describe("VarselService") {
            it("Should store utsendt varsel") {
                every { dkifClient.person(any()).kanVarsles } returns true

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

            it("Should find every oppfolging varsel to be sent") {
                createMockdataForFnr(
                    fnr = "12345678910",
                    activeSykmelding = true,
                    gjenstaendeSykedager = "70",
                    forelopigBeregnetSlutt = LocalDate.now().plusDays(50),
                )

                createMockdataForFnr(
                    fnr = "12345678911",
                    activeSykmelding = true,
                    gjenstaendeSykedager = "70",
                    forelopigBeregnetSlutt = LocalDate.now().plusDays(50),
                )

                val merOppfolgingVarselToBeSent = varselService.findMerOppfolgingVarselToBeSent()

                merOppfolgingVarselToBeSent.size shouldBe 2
            }

            it("Should not store utsendt varsel if pdfgen fails") {
                every { pdfgenService.getSenOppfolgingLandingPdf(any(), any()) } throws Exception("Help me")

                varselService.sendMerOppfolgingVarsel(
                    MerOppfolgingVarselDTO(
                        personIdent = "12345678910",
                        utbetalingId = "utbetalingId",
                        sykmeldingId = "sykmeldingId",
                    ),
                )

                val utsendtVarsel = varselService.getUtsendtVarsel("12345678910")

                utsendtVarsel shouldBe null
            }

            it("Should not store utsendt varsel journalforing fails") {
                every { dkifClient.person(any()).kanVarsles } returns true
                every { pdfgenService.getSenOppfolgingLandingPdf(any(), any()) } returns ByteArray(1)
                every {
                    dokarkivClient.postSingleDocumentToDokarkiv(any(), any(), any(), any(), any(), any())
                } throws Exception("Help me")

                varselService.sendMerOppfolgingVarsel(
                    MerOppfolgingVarselDTO(
                        personIdent = "12345678910",
                        utbetalingId = "utbetalingId",
                        sykmeldingId = "sykmeldingId",
                    ),
                )

                val utsendtVarsel = varselService.getUtsendtVarsel("12345678910")

                utsendtVarsel shouldBe null
            }

            it("Should store utsendt varsel post to dokarkiv OK") {
                every { pdfgenService.getSenOppfolgingLandingPdf(any(), any()) } returns ByteArray(1)
                every {
                    dokarkivClient.postSingleDocumentToDokarkiv(any(), any(), any(), any(), any(), any())
                } returns DokarkivResponse(null, "1", null, "status", null)

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
            }

            it("should return utsendt varsel from COPY_UTSENDT_VARSEL_ESYFOVARSEL") {
                val uuid = UUID.randomUUID().toString()
                jdbcTemplate.execute(
                    """
                    INSERT INTO COPY_UTSENDT_VARSEL_ESYFOVARSEL(
                        uuid_esyfovarsel, fnr, type, utsendt_tidspunkt
                    )
                    VALUES ('$uuid', 'test-fnr-01', 'type', current_date - INTERVAL '60 days');
                    """.trimMargin(),
                )

                val utsendtVarsel = varselService.getUtsendtVarsel("test-fnr-01")

                utsendtVarsel shouldNotBe null
                utsendtVarsel?.personIdent shouldBe "test-fnr-01"
            }

            describe(
                "getUtsendtVarsel",
            ) {
                it("should return latest utsendt varsel from COPY_UTSENDT_VARSEL_ESYFOVARSEL") {
                    val fnr = "test-fnr-01"
                    val uuidCopy = UUID.randomUUID().toString()
                    jdbcTemplate.execute(
                        """
                    INSERT INTO COPY_UTSENDT_VARSEL_ESYFOVARSEL(
                        uuid_esyfovarsel, fnr, type, utsendt_tidspunkt
                    )
                    VALUES ('$uuidCopy', '$fnr', 'type', current_date - INTERVAL '60 days');
                        """.trimMargin(),
                    )
                    val uuid = UUID.randomUUID().toString()
                    jdbcTemplate.execute(
                        """
                        INSERT INTO UTSENDT_VARSEL (uuid, person_ident, utsendt_tidspunkt, utbetaling_id, sykmelding_id)
                        VALUES ('$uuid','$fnr' , current_date - INTERVAL '65 days', '123', '123')
                        """.trimIndent().trimMargin(),
                    )

                    val utsendtVarsel = varselService.getUtsendtVarsel(fnr)

                    utsendtVarsel shouldNotBe null
                    utsendtVarsel?.uuid.toString() shouldBe uuidCopy
                }
                it("should return latest utsendt varsel from UTSENDT_VARSEL") {
                    val fnr = "test-fnr-01"
                    val uuidCopy = UUID.randomUUID().toString()
                    jdbcTemplate.execute(
                        """
                    INSERT INTO COPY_UTSENDT_VARSEL_ESYFOVARSEL(
                        uuid_esyfovarsel, fnr, type, utsendt_tidspunkt
                    )
                    VALUES ('$uuidCopy', '$fnr', 'type', current_date - INTERVAL '60 days');
                        """.trimMargin(),
                    )
                    val uuid = UUID.randomUUID().toString()
                    jdbcTemplate.execute(
                        """
                        INSERT INTO UTSENDT_VARSEL (uuid, person_ident, utsendt_tidspunkt, utbetaling_id, sykmelding_id)
                        VALUES ('$uuid','$fnr' , current_date - INTERVAL '55 days', '123', '123')
                        """.trimIndent().trimMargin(),
                    )

                    val utsendtVarsel = varselService.getUtsendtVarsel(fnr)

                    utsendtVarsel shouldNotBe null
                    utsendtVarsel?.uuid.toString() shouldBe uuid
                }
                it("should return null when no valid utsendt varsel is found") {
                    val fnr = "test-fnr-02"
                    val uuidCopy = UUID.randomUUID().toString()
                    jdbcTemplate.execute(
                        """
                    INSERT INTO COPY_UTSENDT_VARSEL_ESYFOVARSEL(
                        uuid_esyfovarsel, fnr, type, utsendt_tidspunkt
                    )
                    VALUES ('$uuidCopy', '$fnr', 'type', current_date - INTERVAL '160 days');
                        """.trimMargin(),
                    )
                    val uuid = UUID.randomUUID().toString()
                    jdbcTemplate.execute(
                        """
                        INSERT INTO UTSENDT_VARSEL (uuid, person_ident, utsendt_tidspunkt, utbetaling_id, sykmelding_id)
                        VALUES ('$uuid','$fnr' , current_date - INTERVAL '150 days', '123', '123')
                        """.trimIndent().trimMargin(),
                    )
                    val utsendtVarsel = varselService.getUtsendtVarsel(fnr)

                    utsendtVarsel shouldBe null
                }
            }

            it("Should not send varsel for those older than max age, and only call PDL once") {
                pdlServer.stubHentPerson(yearsOld = 67)
                val personIdent = "12345678910"
                createMockdataForFnr(
                    fnr = personIdent,
                    activeSykmelding = true,
                    gjenstaendeSykedager = "70",
                    forelopigBeregnetSlutt = LocalDate.now().plusDays(50),
                )

                val varselSomSkalSendes = varselService.findMerOppfolgingVarselToBeSent()
                verify(exactly = 1) { pdlClient.hentPersonstatus(any(), any()) }
                varselSomSkalSendes.size shouldBe 0
                val reason =
                    jdbcTemplate.queryForObject(
                        "SELECT reason FROM SKIP_VARSELUTSENDING WHERE person_ident = '$personIdent'",
                        String::class.java,
                    )
                reason shouldBe "AGE"

                varselService.findMerOppfolgingVarselToBeSent()
                verify(exactly = 1) { pdlClient.hentPersonstatus(any(), any()) }

                varselService.findMerOppfolgingVarselToBeSent()
            }

            it("Should not send varsel for deceased person") {
                pdlServer.stubHentPerson(yearsOld = 55, deceased = true)
                val personIdent = "12345678910"
                createMockdataForFnr(
                    fnr = personIdent,
                    activeSykmelding = true,
                    gjenstaendeSykedager = "70",
                    forelopigBeregnetSlutt = LocalDate.now().plusDays(50),
                )

                val varselSomSkalSendes = varselService.findMerOppfolgingVarselToBeSent()
                varselSomSkalSendes.size shouldBe 0
                val reason =
                    jdbcTemplate.queryForObject(
                        "SELECT reason FROM SKIP_VARSELUTSENDING WHERE person_ident = '$personIdent'",
                        String::class.java,
                    )
                reason shouldBe "DECEASED"

                // Should be stored in skip table, so PDL is not called again
                varselService.findMerOppfolgingVarselToBeSent()
                verify(exactly = 1) { pdlClient.hentPersonstatus(any(), any()) }
            }

            it("Should retry PDL lookup when personstatus is UKJENT") {
                pdlServer.stubHentPersonError()
                val personIdent = "12345678910"
                createMockdataForFnr(
                    fnr = personIdent,
                    activeSykmelding = true,
                    gjenstaendeSykedager = "70",
                    forelopigBeregnetSlutt = LocalDate.now().plusDays(50),
                )

                varselService.findMerOppfolgingVarselToBeSent().size shouldBe 0
                varselService.findMerOppfolgingVarselToBeSent().size shouldBe 0

                verify(exactly = 2) { pdlClient.hentPersonstatus(any(), any()) }
                val antallSkip =
                    jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM SKIP_VARSELUTSENDING WHERE person_ident = '$personIdent'",
                        Int::class.java,
                    )
                antallSkip shouldBe 0
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
