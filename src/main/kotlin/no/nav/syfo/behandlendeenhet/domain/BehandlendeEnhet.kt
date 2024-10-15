package no.nav.syfo.behandlendeenhet.domain

import no.nav.syfo.utils.isProd

const val OVRE_EIKER_ENHETSNUMMER = "0624"
const val ASKER_ENHETSNUMMER = "0220"
const val NORDRE_AKER_ENHETSNUMMER = "0331"

data class BehandlendeEnhet(
    var enhetId: String,
    var navn: String,
)

fun BehandlendeEnhet.isPilot(): Boolean {
    if (isProd()) {
        return listOf(OVRE_EIKER_ENHETSNUMMER, ASKER_ENHETSNUMMER, NORDRE_AKER_ENHETSNUMMER).contains(this.enhetId)
    }

    return true
}
