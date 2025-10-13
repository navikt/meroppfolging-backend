package no.nav.syfo.kartlegging

import jakarta.annotation.PostConstruct
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.auth.getFnr
import no.nav.syfo.kartlegging.domain.KandidatStatusResponse
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalRequest
import no.nav.syfo.kartlegging.domain.PersistedKartleggingssporsmal
import no.nav.syfo.kartlegging.exception.NotKandidatException
import no.nav.syfo.kartlegging.service.KandidatService
import no.nav.syfo.kartlegging.service.KartleggingssporsmalService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
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
    val kartleggingssporsmalService: KartleggingssporsmalService,
    val kandidatService: KandidatService,
    // Should always be null in prod
    @Value("\${TOKEN_X_GENERATOR_CLIENT_ID:#{null}}")
    val tokenXGeneratorClientId: String? = null,
) {

    lateinit var tokenValidator: TokenValidator

    @PostConstruct
    fun init() {
        tokenValidator = TokenValidator(
            tokenValidationContextHolder,
            listOfNotNull(broFrontendClientId, tokenXGeneratorClientId),
        )
    }

    @PostMapping
    fun postKartleggingssporsmal(
        @RequestBody kartleggingssporsmal: KartleggingssporsmalRequest,
    ): ResponseEntity<PersistedKartleggingssporsmal?> {
        val personIdent = tokenValidator.validateTokenXClaims().getFnr()
        val muligKandidat = kandidatService.getKandidatByFnr(personIdent)
        if (muligKandidat == null || !muligKandidat.isKandidat()) {
            throw NotKandidatException("Personen er ikke kandidat for kartlegging")
        }
        kartleggingssporsmalService.validateFormSnapshot(kartleggingssporsmal.formSnapshot)
        val persisted = kartleggingssporsmalService.persistAndPublishKartleggingssporsmal(muligKandidat, kartleggingssporsmal)

        return ResponseEntity
            .ok(persisted)
    }

    @GetMapping("/kandidat-status", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getKandidatStatus(): ResponseEntity<KandidatStatusResponse> {
        val personIdent = tokenValidator.validateTokenXClaims().getFnr()

        val muligKandidat = kandidatService.getKandidatByFnr(personIdent)
        if (muligKandidat?.isKandidat() != true) {
            return ResponseEntity
                .ok()
                .body(
                    KandidatStatusResponse(
                        isKandidat = false,
                        formResponse = null,
                    ),
                )
        }
        val latest = kartleggingssporsmalService
            .getLatestKartleggingssporsmal(muligKandidat.kandidatId)

        return ResponseEntity
            .ok()
            .body(
                KandidatStatusResponse(
                    isKandidat = true,
                    formResponse = latest,
                ),
            )
    }
}
