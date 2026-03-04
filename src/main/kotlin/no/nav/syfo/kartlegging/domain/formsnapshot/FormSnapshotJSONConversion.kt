package no.nav.syfo.kartlegging.domain.formsnapshot

import org.slf4j.LoggerFactory
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ValueDeserializer
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

private val logger = LoggerFactory.getLogger("FormSnapshotJSONConversion")

class FieldSnapshotDeserializer : ValueDeserializer<FieldSnapshot>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): FieldSnapshot {
        val node: JsonNode = p.readValueAsTree<JsonNode>()
        return when (val fieldType = node.get("fieldType").asText()) {
            FormSnapshotFieldType.TEXT.name -> ctxt.readTreeAsValue(
                node,
                TextFieldSnapshot::class.java
            )

            FormSnapshotFieldType.CHECKBOX.name -> ctxt.readTreeAsValue(
                node,
                CheckboxFieldSnapshot::class.java
            )

            FormSnapshotFieldType.RADIO_GROUP.name -> ctxt.readTreeAsValue(
                node,
                RadioGroupFieldSnapshot::class.java
            )

            FormSnapshotFieldType.CHECKBOX_SINGLE.name -> ctxt.readTreeAsValue(
                node,
                SingleCheckboxFieldSnapshot::class.java
            )

            else -> throw IllegalArgumentException("Unknown field type: $fieldType")
        }
    }
}

fun FormSnapshot.toJsonString(): String = jacksonObjectMapper().writeValueAsString(this)

fun FormSnapshot.Companion.jsonToFormSnapshot(json: String): FormSnapshot = try {
    jacksonObjectMapper().readValue(json)
} catch (e: Exception) {
    logger.error("Failed to parse FormSnapshot JSON", e)
    throw e
}
