package no.nav.syfo.auth.tokendings

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.io.Serializable
import java.time.LocalDateTime

@Suppress("ConstructorParameterNaming")
@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenXResponse(
    val access_token: String,
    val issued_token_type: String,
    val token_type: String,
    val expires_in: Long,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

fun TokenXResponse.toTokenXToken(): TokenXToken {
    val expiresOn = LocalDateTime.now().plusSeconds(this.expires_in)
    return TokenXToken(
        accessToken = this.access_token,
        expires = expiresOn,
    )
}
