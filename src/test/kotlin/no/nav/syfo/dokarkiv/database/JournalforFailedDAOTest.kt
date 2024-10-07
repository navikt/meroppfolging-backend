package no.nav.syfo.dokarkiv.database

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import no.nav.syfo.LocalApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import java.util.UUID.*

@EmbeddedKafka
@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
class JournalforFailedDAOTest : DescribeSpec() {
    @Autowired
    private lateinit var journalforFailedDAO: JournalforFailedDAO

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    val personIdent = "12121212121"
    val pdf = ByteArray(1)
    val varselUUID = randomUUID().toString()
    val error = "Message: reason of fail"

    init {
        extension(SpringExtension)

        beforeTest {
            jdbcTemplate.execute("TRUNCATE TABLE JOURNALFORING_FAILED CASCADE")
        }

        it("Create journalfor failed") {
            journalforFailedDAO.persistJournalforFailed(personIdent, pdf, varselUUID, error)

            val failedJournalforingRecords = journalforFailedDAO.fetchJournalforingFailed()
            val failedJournalforing = failedJournalforingRecords?.find { it.varselUuid == varselUUID }

            assert(failedJournalforingRecords?.size == 1)
            assert(failedJournalforing?.uuid == varselUUID)
            assert(failedJournalforing?.personIdent == personIdent)
        }

        it("Duplicate insert is not causing exception") {
            journalforFailedDAO.persistJournalforFailed(personIdent, pdf, varselUUID, error)
            journalforFailedDAO.persistJournalforFailed(personIdent, pdf, varselUUID, error)

            val failedJournalforingRecords = journalforFailedDAO.fetchJournalforingFailed()
            val failedJournalforing = failedJournalforingRecords?.find { it.varselUuid == varselUUID }

            assert(failedJournalforingRecords?.size == 1)
            assert(failedJournalforing?.uuid == varselUUID)
            assert(failedJournalforing?.personIdent == personIdent)
        }

        it("Delete journalforing failed") {
            journalforFailedDAO.persistJournalforFailed(personIdent, pdf, varselUUID, error)
            journalforFailedDAO.persistJournalforFailed(personIdent, pdf, randomUUID().toString(), error)

            val failedJournalforingRecords = journalforFailedDAO.fetchJournalforingFailed()

            assert(failedJournalforingRecords?.size == 2)
            journalforFailedDAO.deleteJournalforFailed(varselUUID)

            assert(failedJournalforingRecords?.size == 1)
        }

        it("Journalforing failed with PDF null is allowed") {
            journalforFailedDAO.persistJournalforFailed(personIdent, null, varselUUID, error)

            val failedJournalforingRecords = journalforFailedDAO.fetchJournalforingFailed()
            val failedJournalforing = failedJournalforingRecords?.find { it.varselUuid == varselUUID }

            assert(failedJournalforingRecords?.size == 1)
            assert(failedJournalforing?.uuid == varselUUID)
        }
    }
}
