package no.nav.syfo.senoppfolging.v2

import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearAllMocks
import io.mockk.mockk
import junit.framework.TestCase
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.besvarelse.database.ResponseDao

class SenOppfolgingVeilederADControllerV2Test : DescribeSpec(
{
    val tokenValidationContextHolder = mockk<TokenValidationContextHolder>(relaxed = true)
    val responseDao = mockk<ResponseDao>(relaxed = true)

    val controller = SenOppfolgingVeilederADControllerV2(
        tokenValidationContextHolder = tokenValidationContextHolder,
        responseDao = responseDao,
        )

    val fnr = "12345678910"

    beforeTest {
        clearAllMocks()
    }

    describe("Get form response") {
        it("returns OK") {
            val response = controller.getFormResponse(fnr)
            TestCase.assertEquals("OK", response)
        }
    }
},
)
