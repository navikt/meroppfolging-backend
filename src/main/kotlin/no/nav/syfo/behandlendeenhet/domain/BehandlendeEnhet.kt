package no.nav.syfo.behandlendeenhet.domain

const val OVRE_EIKER_ENHETSNUMMER = "0624"
const val TEST_ENHETSNUMMER = "0314"

data class BehandlendeEnhet(
    var enhetId: String,
    var navn: String,
)

fun BehandlendeEnhet.isPilot(isProd: Boolean): Boolean {
    if (isProd) {
        return listOf(OVRE_EIKER_ENHETSNUMMER).contains(this.enhetId)
    }

    return listOf(TEST_ENHETSNUMMER).contains(this.enhetId)
}
