package no.nav.syfo.kartlegging.service

import no.nav.syfo.dokarkiv.DokarkivClient
import no.nav.syfo.dokarkiv.domain.Distribusjonskanal
import no.nav.syfo.kartlegging.database.KartleggingssporsmalDAO
import no.nav.syfo.kartlegging.domain.Kartleggingssporsmal
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalRequest
import no.nav.syfo.kartlegging.domain.PersistedKartleggingssporsmal
import no.nav.syfo.kartlegging.domain.formsnapshot.FieldSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.FormSnapshot
import no.nav.syfo.kartlegging.domain.formsnapshot.FormSnapshotFieldType
import no.nav.syfo.kartlegging.domain.formsnapshot.validateFields
import no.nav.syfo.kartlegging.exception.InvalidFormException
import no.nav.syfo.kartlegging.kafka.KartleggingssvarEvent
import no.nav.syfo.kartlegging.kafka.KartleggingssvarKafkaProducer
import no.nav.syfo.logger
import no.nav.syfo.syfoopppdfgen.PdfgenService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class KartleggingssporsmalService(
    private val kartleggingssporsmalDAO: KartleggingssporsmalDAO,
    private val kafkaProducer: KartleggingssvarKafkaProducer,
    private val pdfgenService: PdfgenService,
    private val dokarkivClient: DokarkivClient,
) {

    private val logger = logger()

    fun getLatestKartleggingssporsmal(kandidatId: UUID): PersistedKartleggingssporsmal? =
        kartleggingssporsmalDAO.getLatestKartleggingssporsmalByKandidatId(kandidatId)

    fun persistAndPublishKartleggingssporsmal(
        kandidat: KartleggingssporsmalKandidat,
        kartleggingssporsmalRequest: KartleggingssporsmalRequest
    ): PersistedKartleggingssporsmal {
        val persistedKartleggingssporsmal = kartleggingssporsmalDAO.persistKartleggingssporsmal(
            Kartleggingssporsmal(
                fnr = kandidat.personIdent,
                kandidatId = kandidat.kandidatId,
                formSnapshot = kartleggingssporsmalRequest.formSnapshot
            ),
        )
        kafkaProducer.publishResponse(
            KartleggingssvarEvent(
                personident = persistedKartleggingssporsmal.fnr,
                kandidatId = persistedKartleggingssporsmal.kandidatId,
                svarId = persistedKartleggingssporsmal.uuid,
                createdAt = persistedKartleggingssporsmal.createdAt,
            )
        )
        return persistedKartleggingssporsmal
    }

    fun getKartleggingssporsmalByUuid(uuid: UUID): PersistedKartleggingssporsmal? =
        kartleggingssporsmalDAO.getKartleggingssporsmalByUuid(uuid)

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
                    throw IllegalArgumentException(
                        "Field with id: $requiredFieldId has incorrect type. Expected: $requiredFieldType, Found: ${fieldSnapshot.fieldType}"
                    )
                }
            }
            formSnapshot.validateFields()
        } catch (e: Exception) {
            logger.warn("Invalid form in request body: ${e.message}")
            throw InvalidFormException(e.message ?: "Invalid form", e)
        }
    }

    fun getKartleggingssporsmalNotJournaled(): List<PersistedKartleggingssporsmal> =
        kartleggingssporsmalDAO.getKartleggingssporsmalNotJournaled()

    fun jornalforKartleggingssporsmal(kartleggingssporsmal: PersistedKartleggingssporsmal,) {
        val uuid = kartleggingssporsmal.uuid
        val createdAt = kartleggingssporsmal.createdAt
        try {
            val pdf = pdfgenService.getKartleggingsPdf(kartleggingssporsmal, createdAt)
                ?: throw IllegalStateException("Failed to generate PDF for kartleggingssporsmal")

            val response = dokarkivClient.postSingleDocumentToDokarkiv(
                fnr = kartleggingssporsmal.fnr,
                pdf = pdf,
                eksternReferanseId = uuid.toString(),
                title = "Dine svar til skjema om kartleggingsspørsmål",
                filnavn = "svar-kartleggingsspormsal",
                kanal = Distribusjonskanal.NAV_NO_UTEN_VARSLING,
            )
            response?.journalpostId?.let {
                kartleggingssporsmalDAO.setJournalpostIdForKartleggingssporsmal(uuid, it)
            }
        } catch (e: Exception) {
            logger.error("Journalføring av kartleggingssporsmal $uuid feilet: ${e.message}", e)
        }
    }
}
