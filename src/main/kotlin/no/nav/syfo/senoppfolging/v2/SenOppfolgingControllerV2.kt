package no.nav.syfo.senoppfolging.v2

import jakarta.annotation.PostConstruct
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenUtil
import no.nav.syfo.auth.TokenUtil.TokenIssuer.TOKENX
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.auth.getFnr
import no.nav.syfo.logger
import no.nav.syfo.senoppfolging.exception.AlreadyRespondedException
import no.nav.syfo.senoppfolging.exception.NoAccessToSenOppfolgingException
import no.nav.syfo.senoppfolging.exception.NoUtsendtVarselException
import no.nav.syfo.senoppfolging.service.SenOppfolgingService
import no.nav.syfo.senoppfolging.service.UserAccess
import no.nav.syfo.senoppfolging.service.UserAccessError
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingDTOV2
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingStatusDTOV2
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v2/senoppfolging")
@ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
class SenOppfolgingControllerV2(
    @Value("\${MEROPPFOLGING_FRONTEND_CLIENT_ID}")
    val merOppfolgingFrontendClientId: String,
    @Value("\${ESYFO_PROXY_CLIENT_ID}")
    val esyfoProxyClientId: String,
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val senOppfolgingService: SenOppfolgingService,
) {
    lateinit var tokenValidator: TokenValidator
    private val log = logger()

    @PostConstruct
    fun init() {
        tokenValidator =
            TokenValidator(tokenValidationContextHolder, listOf(merOppfolgingFrontendClientId, esyfoProxyClientId))
    }

    @GetMapping("/status", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun status(): SenOppfolgingStatusDTOV2 {
        val personIdent = tokenValidator.validateTokenXClaims().getFnr()
        val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, TOKENX)

        return senOppfolgingService.prepareAndBuildSenOppfolgingStatusDTOV2(personIdent, token)
    }

    @PostMapping("/submitform")
    @ResponseBody
    fun submitForm(
        @RequestBody senOppfolgingSvar: SenOppfolgingDTOV2,
    ) {
        val personIdent = tokenValidator.validateTokenXClaims().getFnr()
        val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, TOKENX)

        val userAccess = senOppfolgingService.getUserAccess(personIdent, token)

        val varsel =
            when (userAccess) {
                is UserAccess.Good -> userAccess.varsel
                is UserAccess.Error -> {
                    when (userAccess.error) {
                        UserAccessError.NoUtsendtVarsel -> throw NoUtsendtVarselException().also {
                            log.error("User has no valid varsel.")
                        }
                        UserAccessError.NoAccessToSenOppfolging -> throw NoAccessToSenOppfolgingException().also {
                            log.error("User is not in a oppfolgingtilfelle + 16 days.")
                        }
                    }
                }
            }
        senOppfolgingService.getResponseOrNull(varsel, personIdent)?.let {
            log.error("User has already responded in the last 3 months.")
            throw AlreadyRespondedException()
        }

        senOppfolgingService.handleSubitForm(
            personIdent = personIdent,
            senOppfolgingForm = senOppfolgingSvar,
            varsel = varsel,
        )
    }
}
