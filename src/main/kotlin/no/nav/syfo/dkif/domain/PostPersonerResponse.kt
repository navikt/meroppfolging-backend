package no.nav.syfo.dkif.domain

data class PostPersonerResponse(
    val personer: Map<String, Kontaktinfo> = mapOf(),
    val feil: Map<String, String> = mapOf(),
)
