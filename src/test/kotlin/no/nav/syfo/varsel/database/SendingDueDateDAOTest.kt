package no.nav.syfo.varsel.database

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.BEFORE_CLASS
import no.nav.syfo.LocalApplication
import no.nav.syfo.varsel.MerOppfolgingVarselDTO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import java.time.LocalDate

@AutoConfigureEmbeddedDatabase(provider = ZONKY, refresh = BEFORE_CLASS)
@EmbeddedKafka
@TestConfiguration
@SpringBootTest(classes = [LocalApplication::class])
class SendingDueDateDAOTest : DescribeSpec() {
    @Autowired
    private lateinit var sendingDueDateDAO: SendingDueDateDAO

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    init {
        extension(SpringExtension)

        beforeTest {
            jdbcTemplate.execute("TRUNCATE TABLE SENDING_DUE_DATE CASCADE")
        }

        val personIdent = "12345678910"
        val utbetalingId = "123"
        val sykmeldingId = "321"

        val dto = MerOppfolgingVarselDTO(
            personIdent,
            utbetalingId,
            sykmeldingId,
        )

        it("Store due date") {
            sendingDueDateDAO.persistSendingDueDate(dto)

            val dueDate = sendingDueDateDAO.getSendingDueDate(personIdent, sykmeldingId, utbetalingId)

            dueDate shouldNotBe null
            dueDate?.toLocalDate() shouldBe LocalDate.now().plusDays(2)
        }

        it("Inserting duplicate dto should not cause exception") {
            sendingDueDateDAO.persistSendingDueDate(dto)

            val dueDate = sendingDueDateDAO.getSendingDueDate(personIdent, sykmeldingId, utbetalingId)
            sendingDueDateDAO.getSendingDueDate(personIdent, sykmeldingId, utbetalingId)

            dueDate shouldNotBe null
            dueDate?.toLocalDate() shouldBe LocalDate.now().plusDays(2)
        }

        it("Delete record") {
            sendingDueDateDAO.persistSendingDueDate(dto)

            val dueDate = sendingDueDateDAO.getSendingDueDate(personIdent, sykmeldingId, utbetalingId)
            sendingDueDateDAO.getSendingDueDate(personIdent, sykmeldingId, utbetalingId)

            dueDate shouldNotBe null
            dueDate?.toLocalDate() shouldBe LocalDate.now().plusDays(2)

            sendingDueDateDAO.deleteSendingDueDate(sykmeldingId)

            val dueDateAfterDelete = sendingDueDateDAO.getSendingDueDate(personIdent, sykmeldingId, utbetalingId)
            dueDateAfterDelete shouldBe null
        }

        it("Deleting duplicate dto should not cause exception") {
            sendingDueDateDAO.persistSendingDueDate(dto)

            val dueDate = sendingDueDateDAO.getSendingDueDate(personIdent, sykmeldingId, utbetalingId)
            sendingDueDateDAO.getSendingDueDate(personIdent, sykmeldingId, utbetalingId)

            dueDate shouldNotBe null
            dueDate?.toLocalDate() shouldBe LocalDate.now().plusDays(2)

            sendingDueDateDAO.deleteSendingDueDate(sykmeldingId)
            val dueDateAfterDelete = sendingDueDateDAO.getSendingDueDate(personIdent, sykmeldingId, utbetalingId)
            dueDateAfterDelete shouldBe null
            sendingDueDateDAO.deleteSendingDueDate(sykmeldingId)
            val dueDateAfterDelete2 = sendingDueDateDAO.getSendingDueDate(personIdent, sykmeldingId, utbetalingId)
            dueDateAfterDelete2 shouldBe null
        }
    }
}
