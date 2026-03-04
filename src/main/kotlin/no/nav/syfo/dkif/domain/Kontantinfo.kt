package no.nav.syfo.dkif.domain

import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.jsonMapper

data class Kontaktinfo(val kanVarsles: Boolean?, val reservert: Boolean?,)

object KontaktinfoMapper {

    private val objectMapper = jsonMapper {
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    fun mapPerson(json: String): Kontaktinfo? {
        val jsonNode = objectMapper.readTree(json)

        if (jsonNode.has("feil")) {
            return null
        }

        jsonNode.let {
            return Kontaktinfo(
                it["kanVarsles"]?.asBoolean(),
                it["reservert"]?.asBoolean()
            )
        }
    }
}
