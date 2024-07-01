package no.nav.syfo.senoppfolging.v2

import jakarta.validation.constraints.Pattern
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.NAV_PERSONIDENT_HEADER
import no.nav.syfo.auth.TokenUtil
import no.nav.syfo.auth.TokenUtil.TokenIssuer.AZUREAD
import no.nav.syfo.besvarelse.database.ResponseDao
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@ProtectedWithClaims(issuer = AZUREAD)
@RequestMapping("/api/v2/internad/senoppfolging")
class SenOppfolgingVeilederADControllerV2(
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val responseDao: ResponseDao,
) {
    @GetMapping("/formresponse", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun getFormResponse(@RequestHeader(name = NAV_PERSONIDENT_HEADER) personident: @Pattern(regexp = "^[0-9]{11}$") String): String {
        val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, AZUREAD)
        /*
        - Sjekk veileder-tilgang til person med token
        - Hent siste skjemasvar for person
         */
        return "OK"
    }

}
