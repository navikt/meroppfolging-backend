package no.nav.syfo.auth.azuread

import java.io.Serializable
import java.time.LocalDateTime

data class AzureAdToken(
    val accessToken: String,
    val expires: LocalDateTime
) : Serializable {
    companion object {
        private const val serialVersionUID = 436436324L
    }
}

fun AzureAdToken.isExpired() = this.expires < LocalDateTime.now().plusSeconds(120)
