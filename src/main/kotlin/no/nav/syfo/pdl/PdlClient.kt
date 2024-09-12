package no.nav.syfo.pdl

import no.nav.syfo.auth.azuread.AzureAdClient
import no.nav.syfo.pdl.domain.HentPersonData
import no.nav.syfo.pdl.domain.HentPersonResponse
import no.nav.syfo.pdl.domain.PdlRequest
import no.nav.syfo.pdl.domain.Variables
import no.nav.syfo.pdl.domain.errorMessage
import no.nav.syfo.pdl.domain.getFodselsdato
import no.nav.syfo.utils.isAlderMindreEnnGittAr
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import java.io.FileNotFoundException

@Component
class PdlClient(
    private val azureAdClient: AzureAdClient,
    @Value("\${pdl.url}") private val pdlUrl: String,
    @Value("\${pdl.scope}") private val pdlScope: String,
) {

    private val log: Logger = LoggerFactory.getLogger(PdlClient::class.qualifiedName)

    fun isBrukerYngreEnnGittMaxAlder(ident: String, maxAlder: Int): Boolean {
        val fodselsdato = hentPersonData(ident)?.getFodselsdato()
        if (fodselsdato == null) {
            log.warn(
                "Returnert fødselsdato for en person fra PDL er null. " +
                    "Fortsetter som om bruker er yngre enn $maxAlder år da fødselsdato er ukjent.",
            )
            return true
        } else {
            return isAlderMindreEnnGittAr(fodselsdato, maxAlder)
        }
    }

    private fun hentPersonData(
        personIdent: String,
    ): HentPersonData? {
        val token = azureAdClient.getSystemToken(pdlScope)
        val query = getPdlQuery("/pdl/hentPerson.graphql")
        val request = PdlRequest(query, Variables(personIdent))

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(token)
            add(PDL_BEHANDLINGSNUMMER_HEADER, BEHANDLINGSNUMMER_VURDERE_RETT_TIL_SYKEPENGER)
        }

        return try {
            val response: ResponseEntity<HentPersonResponse> = RestTemplate().exchange(
                pdlUrl,
                HttpMethod.POST,
                HttpEntity(request, headers),
                HentPersonResponse::class.java,
            )

            val pdlPersonResponse = response.body
            if (pdlPersonResponse?.errors.isNullOrEmpty()) {
                pdlPersonResponse?.data
            } else {
                pdlPersonResponse?.errors?.forEach {
                    log.error("Error while requesting person from PersonDataLosningen: ${it.errorMessage()}")
                }
                null
            }
        } catch (e: RestClientResponseException) {
            log.error("Request with url: $pdlUrl failed with response code ${e.statusCode.value()}")
            null
        }
    }

    private fun getPdlQuery(graphQueryResourcePath: String): String {
        return this::class.java.getResource(graphQueryResourcePath)?.readText()?.replace("[\n\r]", "")
            ?: throw FileNotFoundException("Could not found resource: $graphQueryResourcePath")
    }
}
