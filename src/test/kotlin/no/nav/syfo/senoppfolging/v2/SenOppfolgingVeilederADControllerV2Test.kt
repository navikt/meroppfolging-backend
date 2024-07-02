package no.nav.syfo.senoppfolging.v2

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.NoAccess
import no.nav.syfo.auth.azuread.AzureAdClientException
import no.nav.syfo.besvarelse.database.ResponseDao
import no.nav.syfo.veiledertilgang.VeilederTilgangClient

class SenOppfolgingVeilederADControllerV2Test : DescribeSpec(
{
    val tokenValidationContextHolder = mockk<TokenValidationContextHolder>(relaxed = true)
    val responseDao = mockk<ResponseDao>(relaxed = true)
    val veilederTilgangClient = mockk<VeilederTilgangClient>(relaxed = true)

    val controller = SenOppfolgingVeilederADControllerV2(
        tokenValidationContextHolder = tokenValidationContextHolder,
        veilederTilgangClient = veilederTilgangClient,
        responseDao = responseDao,
        )

    val fnr = "12345678910"

    beforeTest {
        clearAllMocks()
    }

    describe("Get form response") {
        it("returns OK when veileder has access to person") {
            every { veilederTilgangClient.hasVeilederTilgangToPerson(any(), any()) } returns true

            val response = controller.getFormResponse(fnr)
            TestCase.assertNotNull(response)
        }

        it("throws NoAccess when veileder denied access to person") {
            every { veilederTilgangClient.hasVeilederTilgangToPerson(any(), any()) } returns false

            val exception = shouldThrow<NoAccess> {
                controller.getFormResponse(fnr)
            }
            TestCase.assertEquals("Veileder har ikke tilgang til person", exception.message)
        }
    }
},
)
