package no.nav.syfo.besvarelse.database

import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import no.nav.syfo.LocalApplication
import no.nav.syfo.besvarelse.database.domain.FormType
import no.nav.syfo.besvarelse.database.domain.QuestionResponse
import no.nav.syfo.domain.PersonIdentNumber
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest(classes = [LocalApplication::class])
class ResponseDaoTest : FunSpec() {
    @Autowired
    private lateinit var responseDao: ResponseDao

    init {
        extension(SpringExtension)

        test("Create besvarelse") {
            val personIdent = PersonIdentNumber("12345678910")
            responseDao.saveFormResponse(
                personIdent,
                listOf(QuestionResponse("UTDANNING", "test", "SVAR_ID", "test")),
                FormType.SEN_OPPFOLGING_V1
            )

            val questionResponses = responseDao.find(
                personIdent,
                FormType.SEN_OPPFOLGING_V1,
                LocalDate.now().minusDays(1)
            )
            assert(questionResponses.size == 1)
        }

        test("Find empty") {
            val personIdent = PersonIdentNumber("12345678911")

            val questionResponses = responseDao.find(
                personIdent,
                FormType.SEN_OPPFOLGING_V1,
                LocalDate.now().minusDays(1)
            )
            assert(questionResponses.isEmpty())
        }
    }
}
