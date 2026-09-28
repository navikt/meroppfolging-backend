package no.nav.syfo.kartlegging.service

import net.logstash.logback.marker.Markers.appendEntries
import no.nav.syfo.documentFailureFields
import no.nav.syfo.dokarkiv.DokarkivClient
import no.nav.syfo.dokarkiv.domain.Distribusjonskanal
import no.nav.syfo.kartlegging.database.KartleggingssporsmalDAO
import no.nav.syfo.kartlegging.domain.Kartleggingssporsmal
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalRequest
import no.nav.syfo.kartlegging.domain.PersistedKartleggingssporsmal
import no.nav.syfo.kartlegging.domain.formsnapshot.FormSnapshot
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
        try {
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
        var operation = "generate_kartlegging_pdf"
        var upstream: String? = "syfooppdfgen"
        try {
            val pdf = pdfgenService.getKartleggingsPdf(kartleggingssporsmal, createdAt)
                ?: throw IllegalStateException("Failed to generate PDF for kartleggingssporsmal")

            operation = "create_journalpost"
            upstream = "dokarkiv"
            val response = dokarkivClient.postSingleDocumentToDokarkiv(
                fnr = kartleggingssporsmal.fnr,
                pdf = pdf,
                eksternReferanseId = uuid.toString(),
                title = "Dine svar til skjema om kartleggingsspørsmål",
                filnavn = "svar-kartleggingsspormsal",
                kanal = Distribusjonskanal.NAV_NO_UTEN_VARSLING,
            )
            response?.journalpostId?.let {
                operation = "store_journalpost_id"
                upstream = null
                kartleggingssporsmalDAO.setJournalpostIdForKartleggingssporsmal(uuid, it)
            }
        } catch (e: Exception) {
            logger.error(
                appendEntries(
                    e.documentFailureFields() + buildMap {
                        put("event_type", "kartlegging_journalforing_failed")
                        put("operation", operation)
                        upstream?.let { put("upstream", it) }
                    },
                ),
                "Journalføring av kartleggingsspørsmål feilet",
            )
        }
    }
}
