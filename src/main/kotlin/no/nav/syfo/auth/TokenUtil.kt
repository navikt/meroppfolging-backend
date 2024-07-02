package no.nav.syfo.auth

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtTokenClaims

object TokenUtil {
    @JvmStatic
    fun getIssuerToken(
        contextHolder: TokenValidationContextHolder,
        issuer: String,
    ): String {
        val context = contextHolder.getTokenValidationContext()
        return context.getJwtToken(issuer)?.encodedToken
            ?: throw TokenValidationException("Klarte ikke hente token fra issuer: $issuer")
    }

    class TokenValidationException(message: String) : RuntimeException(message)

    object TokenIssuer {
        const val TOKENX = "tokenx"
        const val AZUREAD = "azuread"
    }
}

fun JwtTokenClaims.getFnr(): String {
    return this.getStringClaim("pid")
}
