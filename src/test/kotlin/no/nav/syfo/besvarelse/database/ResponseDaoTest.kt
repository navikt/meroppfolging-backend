package no.nav.syfo.besvarelse.database

import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import no.nav.syfo.LocalApplication
import no.nav.syfo.besvarelse.database.domain.FormType
import no.nav.syfo.besvarelse.database.domain.QuestionResponse
import no.nav.syfo.domain.PersonIdentNumber
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [LocalApplication::class])
class ResponseDaoTest : FunSpec() {
    @Autowired
    private lateinit var responseDao: ResponseDao

    init {
        extension(SpringExtension)

        test("Create response") {
            responseDao.saveFormResponse(
                PersonIdentNumber("12345678910"),
                listOf(QuestionResponse("utdanning", "test", "SVAR_ID", "test")),
                FormType.SEN_OPPFOLGING_V1,
            )
        }
    }
}
