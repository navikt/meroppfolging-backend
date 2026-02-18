package no.nav.syfo.pdl

import no.nav.syfo.auth.azuread.AzureAdClient
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
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

@Component
class PdlClient(
    private val azureAdClient: AzureAdClient,
    @Value("\${pdl.url}") private val pdlUrl: String,
    @Value("\${pdl.scope}") private val pdlScope: String,
) {

    private val log: Logger = LoggerFactory.getLogger(PdlClient::class.qualifiedName)

    fun hentPersonstatus(ident: String, maxAlder: Int): PersonstatusResultat {
        val personDataResultat = hentPersonData(ident)
        if (personDataResultat.feilet) {
            return PersonstatusResultat.Ukjent
        }

        val personData = personDataResultat.data
        if (personData == null) {
            log.warn("Mottok tom persondata fra PDL, personstatus settes til UKJENT")
            return PersonstatusResultat.Ukjent
        }

        val fodselsdato = personData.hentFodselsdato()
        if (personData.erDoed()) {
            return PersonstatusResultat.Doed(
                fodselsdato = fodselsdato,
            )
        }

        if (fodselsdato == null) {
            log.warn(
                "Returnert fødselsdato for en person fra PDL er null. " +
                    "Fortsetter som om bruker er yngre enn $maxAlder år da fødselsdato er ukjent.",
            )
            return PersonstatusResultat.Levende(
                erUnderMaksAlder = true,
                fodselsdato = null,
            )
        }

        return PersonstatusResultat.Levende(
            erUnderMaksAlder = isAlderMindreEnnGittAr(fodselsdato, maxAlder),
            fodselsdato = fodselsdato,
        )
    }

    private fun hentPersonData(personIdent: String,): HentPersonDataResultat {
        val token = azureAdClient.getSystemToken(pdlScope)
        val request = PdlRequest(createPdlQuery(), Variables(personIdent))

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
            if (pdlPersonResponse == null) {
                log.error("Mottok tom respons fra PersonDataLosningen")
                HentPersonDataResultat(data = null, feilet = true)
            } else if (pdlPersonResponse.errors.isNullOrEmpty()) {
                HentPersonDataResultat(data = pdlPersonResponse.data, feilet = false)
            } else {
                pdlPersonResponse.errors?.forEach {
                    log.error("Error while requesting person from PersonDataLosningen: ${it.errorMessage()}")
                }
                HentPersonDataResultat(data = null, feilet = true)
            }
        } catch (e: RestClientResponseException) {
            log.error("Request with url: $pdlUrl failed with response code ${e.statusCode.value()}")
            HentPersonDataResultat(data = null, feilet = true)
        } catch (e: Exception) {
            log.error("Request with url: $pdlUrl failed with exception: ${e.message}", e)
            HentPersonDataResultat(data = null, feilet = true)
        }
    }

    private fun parsePDLDate(date: String): LocalDate {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return LocalDate.parse(date, formatter)
    }

    private fun isAlderMindreEnnGittAr(fodselsdato: String, maxAlder: Int): Boolean {
        val parsedFodselsdato = parsePDLDate(fodselsdato)

        return Period.between(parsedFodselsdato, LocalDate.now()).years < maxAlder
    }

    private fun createPdlQuery(): String =
        this::class.java.getResource("/pdl/hentPerson.graphql")?.readText()?.replace("[\n\r]", "")
            ?: throw FileNotFoundException("Could not find resource for hentPerson.graphql")

    sealed interface PersonstatusResultat {
        data class Levende(
            val erUnderMaksAlder: Boolean,
            val fodselsdato: String?,
        ) : PersonstatusResultat

        data class Doed(
            val fodselsdato: String?,
        ) : PersonstatusResultat

        data object Ukjent : PersonstatusResultat
    }

    private data class HentPersonDataResultat(
        val data: HentPersonData?,
        val feilet: Boolean,
    )
}
