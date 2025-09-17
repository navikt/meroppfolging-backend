package no.nav.syfo.kartlegging.service

import no.nav.syfo.kartlegging.database.KartleggingssporsmalDAO
import no.nav.syfo.kartlegging.domain.Kartleggingssporsmal
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalRequest
import no.nav.syfo.kartlegging.domain.PersistedKartleggingssporsmal
import no.nav.syfo.kartlegging.domain.formsnapshot.FieldSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.FormSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.FormSnapshotFieldType
import no.nav.syfo.kartlegging.domain.formsnapshot.validateFields
import org.springframework.stereotype.Service

@Service
class KartleggingssporsmalService(
    private val kartleggingssporsmalDAO: KartleggingssporsmalDAO,
) {

    fun getLatestKartleggingssporsmal(personIdent: String): PersistedKartleggingssporsmal? {
        return kartleggingssporsmalDAO.getLatestKartleggingssporsmalByFnr(personIdent)
    }

    fun persistKartleggingssporsmal(personIdent: String, kartleggingssporsmalRequest: KartleggingssporsmalRequest) {
        kartleggingssporsmalDAO.persistKartleggingssporsmal(
            Kartleggingssporsmal(
                fnr = personIdent,
                formSnapshot = kartleggingssporsmalRequest.formSnapshot
            )
        )
    }
    fun validateFormSnapshot(formSnapshot: FormSnapshot) {
        val requiredFieldIds = listOf(
            "hvorSannsynligTilbakeTilJobben" to FormSnapshotFieldType.RADIO_GROUP,
            "samarbeidOgRelasjonTilArbeidsgiver" to FormSnapshotFieldType.RADIO_GROUP,
            "naarTilbakeTilJobben" to FormSnapshotFieldType.RADIO_GROUP,
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
