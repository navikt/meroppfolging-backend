package no.nav.syfo.pdl

data class HentPersonResponse(
    val errors: List<PdlError>?,
    val data: HentPersonData
)

data class HentPersonData(
    val hentPerson: HentPerson
)

fun HentPersonData.getFodselsdato(): String? {
    return this.hentPerson.foedselsdato.first().foedselsdato
}

data class HentPerson(
    val foedselsdato: List<Foedselsdato>,
    val navn: List<Navn>
)

data class Foedselsdato(
    val foedselsdato: String?
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
)

data class PdlError(
    val message: String,
    val locations: List<PdlErrorLocation>,
    val path: List<String>?,
    val extensions: PdlErrorExtension
)

data class PdlErrorLocation(
    val line: Int?,
    val column: Int?
)

data class PdlErrorExtension(
    val code: String?,
    val classification: String
)

fun PdlError.errorMessage(): String {
    return "${this.message} with code: ${extensions.code} and classification: ${extensions.classification}"
}
