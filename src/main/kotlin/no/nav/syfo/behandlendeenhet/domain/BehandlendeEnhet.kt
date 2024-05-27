package no.nav.syfo.behandlendeenhet.domain

data class BehandlendeEnhet(
    var enhetId: String,
    var navn: String
)

fun BehandlendeEnhet.isPilot(): Boolean {
    return this.enhetId == "0316"
}
