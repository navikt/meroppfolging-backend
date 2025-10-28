package no.nav.syfo.kartlegging

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.NoAccess
import no.nav.syfo.auth.TokenUtil
import no.nav.syfo.auth.TokenUtil.TokenIssuer.AZUREAD
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.kartlegging.domain.PersistedKartleggingssporsmal
import no.nav.syfo.kartlegging.exception.KandidatNotFoundException
import no.nav.syfo.kartlegging.exception.UserResponseNotFoundException
import no.nav.syfo.kartlegging.service.KandidatService
import no.nav.syfo.kartlegging.service.KartleggingssporsmalService
import no.nav.syfo.veiledertilgang.VeilederTilgangClient
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@ProtectedWithClaims(issuer = AZUREAD)
@RequestMapping("/api/v1/internad/kartleggingssporsmal")
class KartleggingssporsmalVeilederControllerV1(
    val tokenValidationContextHolder: TokenValidationContextHolder,
    val veilederTilgangClient: VeilederTilgangClient,
    val kartleggingssporsmalService: KartleggingssporsmalService,
    private val kandidatService: KandidatService
) {

    @GetMapping("/{uuid}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getKartleggingssporsmal(@PathVariable uuid: UUID): ResponseEntity<PersistedKartleggingssporsmal?> {
        val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, AZUREAD)

        val response = kartleggingssporsmalService.getKartleggingssporsmalByUuid(uuid)
            ?: throw UserResponseNotFoundException("Fant ikke kartleggingsspørsmål med uuid: $uuid")

        val hasVeilederTilgangToPerson = veilederTilgangClient.hasVeilederTilgangToPerson(
            token = token,
            personident = PersonIdentNumber(response.fnr),
        )
        if (!hasVeilederTilgangToPerson) {
            throw NoAccess("Veileder har ikke tilgang til person")
        }
        return ResponseEntity
            .ok(response)
    }

    @GetMapping("/kandidat/{kandidatId}/svar", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getKartleggingssporsmalSvar(@PathVariable kandidatId: UUID): ResponseEntity<PersistedKartleggingssporsmal?> {
        val token = TokenUtil.getIssuerToken(tokenValidationContextHolder, AZUREAD)

        val kandidat = kandidatService.getKandidatByKandidatId(kandidatId)
            ?: throw KandidatNotFoundException("Fant ikke kandidat med kandidatId: $kandidatId")

        val hasVeilederTilgangToPerson = veilederTilgangClient.hasVeilederTilgangToPerson(
            token = token,
            personident = PersonIdentNumber(kandidat.personIdent),
        )
        if (!hasVeilederTilgangToPerson) {
            throw NoAccess("Veileder har ikke tilgang til person")
        }

        val response = kartleggingssporsmalService.getLatestKartleggingssporsmal(kandidat.kandidatId)

        return ResponseEntity
            .ok(response)
    }

}
