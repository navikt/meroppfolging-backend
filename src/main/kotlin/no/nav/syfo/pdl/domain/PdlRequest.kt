package no.nav.syfo.pdl.domain

data class PdlRequest(
    val query: String,
    val variables: Variables
)

data class Variables(
    val ident: String
)
