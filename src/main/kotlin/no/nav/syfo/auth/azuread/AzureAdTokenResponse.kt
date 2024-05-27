package no.nav.syfo.auth.azuread

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.io.Serializable
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
@Suppress("ConstructorParameterNaming")
data class AzureAdTokenResponse(
    val access_token: String,
    val expires_in: Long,
) : Serializable {
    companion object {
        private const val serialVersionUID = 4346464L
    }
}

fun AzureAdTokenResponse.toAzureAdToken(): AzureAdToken {
    val expiresOn = LocalDateTime.now().plusSeconds(this.expires_in)
    return AzureAdToken(
        accessToken = this.access_token,
        expires = expiresOn,
    )
}
