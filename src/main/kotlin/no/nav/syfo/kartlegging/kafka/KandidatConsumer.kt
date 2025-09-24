package no.nav.syfo.kartlegging.kafka

import no.nav.syfo.kartlegging.database.KandidatDAO
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartlegging.domain.KandidatStatus
import no.nav.syfo.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class KandidatConsumer(
    private val kandidatDAO: KandidatDAO
) {
    private val log = logger()

    @KafkaListener(topics = [KANDIDAT_TOPIC], containerFactory = "kandidatKafkaListenerContainerFactory")
    fun listen(
        records: List<ConsumerRecord<String, KandidatKafkaEvent?>>,
        ack: Acknowledgment,
    ) {
        try {
            log.info("Received ${records.size} kandidater from $KANDIDAT_TOPIC")
            records.forEach { record ->
                record.value()?.let {
                    when (it.status) {
                        KandidatStatus.IKKE_KANDIDAT -> {
                            log.info("Received record for kandidatId: ${it.kandidatId} where status is IKKE_KANDIDAT. Skipping...")
                        }
                        KandidatStatus.KANDIDAT -> {
                            log.info("Storing kandidat for kandidatId: ${it.kandidatId}")
                            kandidatDAO.persistKandidat(it.toKandidat())
                        }
                    }
                }
            }
            log.info("Committing offset for topic $KANDIDAT_TOPIC")
            ack.acknowledge()
        } catch (e: Exception) {
            log.error("Unexpected error: ${e.message}", e)
        }
    }

    companion object {
        // TODO: Update topic name when available
        const val KANDIDAT_TOPIC = "teamsykefravr.kandidat"
    }

    fun KandidatKafkaEvent.toKandidat() = KartleggingssporsmalKandidat(
        personIdent = this.personIdent,
        kandidatId = this.kandidatId,
        status = this.status,
        createdAt = this.createdAt,
    )
}
