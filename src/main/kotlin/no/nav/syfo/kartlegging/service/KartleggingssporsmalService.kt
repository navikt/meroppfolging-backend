package no.nav.syfo.kartlegging.service

import no.nav.syfo.kartlegging.database.KartleggingssporsmalDAO
import no.nav.syfo.kartlegging.domain.Kartleggingssporsmal
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalRequest
import no.nav.syfo.kartlegging.domain.PersistedKartleggingssporsmal
import no.nav.syfo.kartlegging.domain.formsnapshot.FieldSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.FormSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.FormSnapshotFieldType
import no.nav.syfo.kartlegging.domain.formsnapshot.logger
import no.nav.syfo.kartlegging.domain.formsnapshot.validateFields
import no.nav.syfo.kartlegging.exception.InvalidFormException
import no.nav.syfo.kartlegging.kafka.KartleggingssvarEvent
import no.nav.syfo.kartlegging.kafka.KartleggingssvarKafkaProducer
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class KartleggingssporsmalService(
    private val kartleggingssporsmalDAO: KartleggingssporsmalDAO,
    private val kafkaProducer: KartleggingssvarKafkaProducer
) {

    fun getLatestKartleggingssporsmal(personIdent: String): PersistedKartleggingssporsmal? {
        return kartleggingssporsmalDAO.getLatestKartleggingssporsmalByFnr(personIdent)
    }

    fun persistAndPublishKartleggingssporsmal(personIdent: String, kartleggingssporsmalRequest: KartleggingssporsmalRequest) {
        val createdAt = Instant.now()
        val uuid = kartleggingssporsmalDAO.persistKartleggingssporsmal(
            Kartleggingssporsmal(
                fnr = personIdent,
                formSnapshot = kartleggingssporsmalRequest.formSnapshot
            ),
            createdAt
        )
        kafkaProducer.publishResponse(
            KartleggingssvarEvent(
                personident = personIdent,
                svarId = uuid,
                createdAt = createdAt,
            )
        )
    }

    fun getKartleggingssporsmalByUuid(uuid: UUID): PersistedKartleggingssporsmal? {
        return kartleggingssporsmalDAO.getKartleggingssporsmalByUuid(uuid)
    }

    fun validateFormSnapshot(formSnapshot: FormSnapshot) {
        val requiredFieldIds = listOf(
            "hvorSannsynligTilbakeTilJobben" to FormSnapshotFieldType.RADIO_GROUP,
            "samarbeidOgRelasjonTilArbeidsgiver" to FormSnapshotFieldType.RADIO_GROUP,
            "naarTilbakeTilJobben" to FormSnapshotFieldType.RADIO_GROUP,
        )

        try {
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
        } catch (e: Exception) {
            logger.warn("Invalid form in request body: ${e.message}")
            throw InvalidFormException(e.message ?: "Invalid form", e)
        }
    }
}
