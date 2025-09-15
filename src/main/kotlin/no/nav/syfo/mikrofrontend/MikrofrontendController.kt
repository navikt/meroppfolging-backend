package no.nav.syfo.mikrofrontend

import jakarta.annotation.PostConstruct
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenUtil
import no.nav.syfo.auth.TokenUtil.TokenIssuer.TOKENX
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.auth.getFnr
import no.nav.syfo.mikrofrontend.domain.MerOppfolgingStatusDTO
import no.nav.syfo.mikrofrontend.service.MikrofrontendService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/mikrofrontend/v1")
@ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
class MikrofrontendController(
    @param:Value("\${ESYFO_PROXY_CLIENT_ID}")
    val esyfoProxyClientId: String,
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val mikrofrontendService: MikrofrontendService,
) {
    lateinit var tokenValidator: TokenValidator

    @PostConstruct
    fun init() {
        tokenValidator =
            TokenValidator(tokenValidationContextHolder, listOf(esyfoProxyClientId))
    }

    @GetMapping("/status", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun status(): MerOppfolgingStatusDTO {
        val personIdent = tokenValidator.validateTokenXClaims().getFnr()
        val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, TOKENX)

        return mikrofrontendService.status(personIdent, token)
    }

}
