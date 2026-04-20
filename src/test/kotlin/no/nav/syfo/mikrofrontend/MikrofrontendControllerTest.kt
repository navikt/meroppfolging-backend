package no.nav.syfo.mikrofrontend

import com.nimbusds.jwt.JWTClaimsSet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.syfo.auth.NoAccess
import no.nav.syfo.mikrofrontend.domain.MerOppfolgingStatusDTO
import no.nav.syfo.mikrofrontend.service.MikrofrontendService
import no.nav.syfo.senoppfolging.v2.domain.ResponseStatus.NO_RESPONSE
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingStatusDTOV2

class MikrofrontendControllerTest :
    DescribeSpec({
        val tokenValidationContextHolder = mockk<TokenValidationContextHolder>(relaxed = true)
        val mikrofrontendService = mockk<MikrofrontendService>(relaxed = true)

        val fnr = "12345678910"

        fun tokenValidationContext(clientId: String): TokenValidationContext {
            val claims =
                JWTClaimsSet.Builder()
                    .claim("client_id", clientId)
                    .claim("pid", fnr)
                    .build()
            val jwtToken = mockk<JwtToken>()
            every { jwtToken.jwtTokenClaims } returns no.nav.security.token.support.core.jwt.JwtTokenClaims(claims)
            every { jwtToken.encodedToken } returns "token"
            return TokenValidationContext(mapOf("tokenx" to jwtToken))
        }

        beforeTest {
            clearAllMocks()
        }

        describe("Auth") {
            it("accepts meroppfolging mikrofrontend client id") {
                val controller =
                    MikrofrontendController(
                        meroppfolgingMicrofrontendClientId = "meroppfolgingMicrofrontendClientId",
                        tokenValidationContextHolder = tokenValidationContextHolder,
                        mikrofrontendService = mikrofrontendService,
                    ).apply {
                        init()
                    }
                every { tokenValidationContextHolder.getTokenValidationContext() } returns
                    tokenValidationContext("meroppfolgingMicrofrontendClientId")
                every { mikrofrontendService.status(fnr, "token") } returns
                    MerOppfolgingStatusDTO.IngenOppfolging(
                        senOppfolgingStatus = SenOppfolgingStatusDTOV2(NO_RESPONSE, null, null, null, null, null, true)
                    )

                val status = controller.status()

                status shouldBe
                    MerOppfolgingStatusDTO.IngenOppfolging(
                        senOppfolgingStatus = SenOppfolgingStatusDTOV2(NO_RESPONSE, null, null, null, null, null, true)
                    )
            }

            it("rejects unauthorized client id") {
                val controller =
                    MikrofrontendController(
                        meroppfolgingMicrofrontendClientId = "meroppfolgingMicrofrontendClientId",
                        tokenValidationContextHolder = tokenValidationContextHolder,
                        mikrofrontendService = mikrofrontendService,
                    ).apply {
                        init()
                    }
                every { tokenValidationContextHolder.getTokenValidationContext() } returns
                    tokenValidationContext("unauthorizedClientId")

                shouldThrow<NoAccess> {
                    controller.status()
                }
            }
        }
    })
