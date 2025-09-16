package no.nav.syfo.kartlegging.service

import no.nav.syfo.kartlegging.database.KartleggingssporsmalDAO
import no.nav.syfo.kartlegging.domain.Kartleggingssporsmal
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalRequest
import no.nav.syfo.kartlegging.domain.formsnapshot.FieldSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.FormSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.FormSnapshotFieldType
import no.nav.syfo.kartlegging.domain.formsnapshot.RadioGroupFieldSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.validateFields
import org.springframework.stereotype.Service

@Service
class KartleggingssporsmalService(
    private val kartleggingssporsmalDAO: KartleggingssporsmalDAO,
) {

    fun persistKartleggingssporsmal(personIdent: String, kartleggingssporsmalRequest: KartleggingssporsmalRequest) {
        validateFormSnapshot(kartleggingssporsmalRequest.formSnapshot)
        kartleggingssporsmalDAO.persistKartleggingssporsmal(
            Kartleggingssporsmal(
                fnr = personIdent,
                formSnapshot = kartleggingssporsmalRequest.formSnapshot
            )
        )
    }
    private fun validateFormSnapshot(formSnapshot: FormSnapshot) {
        val requiredFieldIds = listOf(
            // TODO: Add correct fieldIds and types
            Pair("fieldId1", FormSnapshotFieldType.RADIO_GROUP),
            Pair("fieldId2", FormSnapshotFieldType.RADIO_GROUP),
            Pair("fieldId3", FormSnapshotFieldType.RADIO_GROUP),
        )
        for ((requiredFieldId, requiredFieldType) in requiredFieldIds) {
            val fieldSnapshot: FieldSnapshot? = formSnapshot.fieldSnapshots.find { it.fieldId == requiredFieldId }
            if (fieldSnapshot == null) {
                throw IllegalArgumentException("Missing required field with id: $requiredFieldId")
            }
            if (fieldSnapshot.fieldType != requiredFieldType) {
                throw IllegalArgumentException("Field with id: $requiredFieldId has incorrect type. Expected: $requiredFieldType, Found: ${fieldSnapshot.fieldType}")
            }
        }
        formSnapshot.validateFields()
    }
}
