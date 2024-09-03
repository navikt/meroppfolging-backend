package no.nav.syfo.varsel

import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.syfo.IntegrationTest
import no.nav.syfo.LocalApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest(classes = [LocalApplication::class])
class VarselDAOTest : IntegrationTest() {

    @Autowired
    private lateinit var varselDAO: VarselDAO

    init {
        extension(SpringExtension)

        describe("storeUtsendtVarsel") {
            it("should store UtsendtVarsel") {
                val fnr = "12345678901"
                val sykepengeDagerId = "123"

                varselDAO.storeUtsendtVarsel(fnr, sykepengeDagerId)
                val storedVarsel = varselDAO.getUtsendtVarsel(fnr)

                storedVarsel shouldNotBe null
                storedVarsel!!.fnr shouldBe fnr
                storedVarsel.utsendtTidspunkt.toLocalDate() shouldBe LocalDate.now()
                storedVarsel.sykepengedagerId shouldBe sykepengeDagerId
            }
        }
    }
}
