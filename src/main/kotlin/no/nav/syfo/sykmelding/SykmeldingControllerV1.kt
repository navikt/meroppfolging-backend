package no.nav.syfo.sykmelding

import jakarta.annotation.PostConstruct
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenUtil
import no.nav.syfo.auth.TokenUtil.getIssuerToken
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.oppfolgingstilfelle.IsOppfolgingstilfelleClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping("/api/v1/sykmelding")
@ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
class SykmeldingControllerV1(
    @Value("\${MEROPPFOLGING_FRONTEND_CLIENT_ID}")
    val merOppfolgingFrontendClientId: String,
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val isOppfolgingstilfelleClient: IsOppfolgingstilfelleClient,
) {
    lateinit var tokenValidator: TokenValidator

    @PostConstruct
    fun init() {
        tokenValidator = TokenValidator(tokenValidationContextHolder, merOppfolgingFrontendClientId)
    }

    @GetMapping("/sykmeldt", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun isSykmeldt(): Boolean {
        tokenValidator.validateTokenXClaims()
        val token = getIssuerToken(tokenValidationContextHolder, TokenUtil.TokenIssuer.TOKENX)
        return isOppfolgingstilfelleClient.isSykmeldt(token)
    }
}
