package no.nav.syfo.domain

data class PersonIdentNumber(val value: String) {
    private val elevenDigits = Regex("^\\d{11}\$")

    init {
        require(elevenDigits.matches(value)) { "Value is not a valid PersonIdentNumber" }
    }
}
