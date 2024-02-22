package no.nav.syfo.senoppfolging

import jakarta.annotation.PostConstruct
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.TokenValidator
import no.nav.syfo.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping("/api/v1")
class SenOppfolgingControllerV1(
    @Value("\${MEROPPFOLGING_FRONTEND_CLIENT_ID}")
    val merOppfolgingFrontendClientId: String,
    val tokenValidationContextHolder: TokenValidationContextHolder,
) {
    lateinit var tokenValidator: TokenValidator
    private val log = logger()

    @PostConstruct
    fun init() {
        tokenValidator = TokenValidator(tokenValidationContextHolder, merOppfolgingFrontendClientId)
    }

    @GetMapping("/mer", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    @ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
    fun getMer(): String {
        return "mer"
    }
}
