package no.nav.syfo.senoppfolging

import jakarta.annotation.PostConstruct
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenUtil
import no.nav.syfo.auth.TokenUtil.TokenIssuer.TOKENX
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.logger
import no.nav.syfo.veilarbregistrering.StartRegistrationDTO
import no.nav.syfo.veilarbregistrering.VeilarbregistreringClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping("/api/v1")
@ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
class SenOppfolgingControllerV1(
    @Value("\${MEROPPFOLGING_FRONTEND_CLIENT_ID}")
    val merOppfolgingFrontendClientId: String,
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val veilarbregistreringClient: VeilarbregistreringClient,
) {
    lateinit var tokenValidator: TokenValidator
    private val log = logger()

    @PostConstruct
    fun init() {
        tokenValidator = TokenValidator(tokenValidationContextHolder, merOppfolgingFrontendClientId)
    }

    @GetMapping("/startregistration", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun startRegistration(): StartRegistrationDTO {
        tokenValidator.validateTokenXClaims()
        val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, TOKENX)
        return veilarbregistreringClient.startRegistration(token)
    }

    @PostMapping("/create")
    @ResponseBody
    fun create(
        @RequestBody sykmeldtRegistrering: SykmeldtRegistrering,
    ) {
        tokenValidator.validateTokenXClaims()
        val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, TOKENX)
        veilarbregistreringClient.completeRegistration(token, sykmeldtRegistrering)
    }
}
