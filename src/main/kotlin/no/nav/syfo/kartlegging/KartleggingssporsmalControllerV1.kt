package no.nav.syfo.kartlegging

import jakarta.annotation.PostConstruct
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.auth.getFnr
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalRequest
import no.nav.syfo.kartlegging.service.KartleggingssporsmalService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/api/v1/kartleggingssporsmal")
@ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
class KartleggingssporsmalControllerV1(
    @Value("\${BRO_FRONTEND_CLIENT_ID}")
    val broFrontendClientId: String,
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val kartleggingssporsmalService: KartleggingssporsmalService
) {

    lateinit var tokenValidator: TokenValidator

    @PostConstruct
    fun init() {
        tokenValidator = TokenValidator(tokenValidationContextHolder, broFrontendClientId)
    }

    @PostMapping
    fun postKartleggingssporsmal(
        @RequestBody kartleggingssporsmal: KartleggingssporsmalRequest,
    ): ResponseEntity<Void> {
        val personIdent = tokenValidator.validateTokenXClaims().getFnr()

        kartleggingssporsmalService.persistKartleggingssporsmal(personIdent, kartleggingssporsmal)

        return ResponseEntity
            .ok()
            .build()
    }
}
