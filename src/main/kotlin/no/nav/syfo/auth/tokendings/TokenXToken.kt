package no.nav.syfo.auth.tokendings

import java.io.Serializable
import java.time.LocalDateTime

data class TokenXToken(
    val accessToken: String,
    val expires: LocalDateTime,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
