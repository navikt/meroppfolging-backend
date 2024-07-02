package no.nav.syfo.senoppfolging.v2

import jakarta.validation.constraints.Pattern
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.NAV_PERSONIDENT_HEADER
import no.nav.syfo.auth.NoAccess
import no.nav.syfo.auth.TokenUtil
import no.nav.syfo.auth.TokenUtil.TokenIssuer.AZUREAD
import no.nav.syfo.besvarelse.database.ResponseDao
import no.nav.syfo.besvarelse.database.domain.FormType
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.veiledertilgang.VeilederTilgangClient
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@ProtectedWithClaims(issuer = AZUREAD)
@RequestMapping("/api/v2/internad/senoppfolging")
class SenOppfolgingVeilederADControllerV2(
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val veilederTilgangClient: VeilederTilgangClient,
    val responseDao: ResponseDao,
) {
    private val cutoffDate = LocalDate.now().minusMonths(3)

    @GetMapping("/formresponse", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun getFormResponse(
        @RequestHeader(
            name = NAV_PERSONIDENT_HEADER
        ) personident:
        @Pattern(regexp = "^[0-9]{11}$")
        String
    ): String {
        val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, AZUREAD)
        val personIdentNumber = PersonIdentNumber(value = personident)
        val hasVeilederTilgangToPerson = veilederTilgangClient.hasVeilederTilgangToPerson(
            token = token,
            personident = personIdentNumber
        )
        if (!hasVeilederTilgangToPerson) {
            throw NoAccess("Veileder har ikke tilgang til person")
        }

        val latestFormResponse = responseDao.findLatestFormResponse(
            personIdent = personIdentNumber,
            formType = FormType.SEN_OPPFOLGING_V2,
            from = cutoffDate,
        )

        // Map to dto
        return latestFormResponse.toString()
    }
}
