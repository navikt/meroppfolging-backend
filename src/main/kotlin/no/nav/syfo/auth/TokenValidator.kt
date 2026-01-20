package no.nav.syfo.auth

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.syfo.exception.AbstractApiError
import no.nav.syfo.exception.LogLevel
import org.springframework.http.HttpStatus

class TokenValidator(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val expectedClientIds: List<String>,
) {

    constructor(
        tokenValidationContextHolder: TokenValidationContextHolder,
        expectedClientId: String,
    ) : this(tokenValidationContextHolder, listOf(expectedClientId))

    fun validateTokenXClaims(): JwtTokenClaims {
        val context = tokenValidationContextHolder.getTokenValidationContext()
        val claims = context.getClaims("tokenx")
        val clientId = claims.getStringClaim("client_id")

        if (!expectedClientIds.contains(clientId)) {
            throw NoAccess("Uventet client id $clientId")
        }
        return claims
    }
}

class NoAccess(override val message: String) :
    AbstractApiError(
        message = message,
        httpStatus = HttpStatus.FORBIDDEN,
        reason = "INGEN_TILGANG",
        loglevel = LogLevel.WARN,
    )
