package no.nav.syfo.kartlegging.kafka

import no.nav.syfo.kartlegging.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartlegging.domain.KandidatStatus
import no.nav.syfo.kartlegging.service.KandidatService
import no.nav.syfo.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class KandidatConsumer(
    private val kandidatService: KandidatService
) {
    private val log = logger()

    @KafkaListener(topics = [KANDIDAT_TOPIC], containerFactory = "kandidatKafkaListenerContainerFactory")
    fun listen(
        records: List<ConsumerRecord<String, KandidatKafkaEvent?>>,
        ack: Acknowledgment,
    ) {
        try {
            log.info("Received ${records.size} kandidater from $KANDIDAT_TOPIC")
            processRecords(records)
            log.info("Committing offset for topic $KANDIDAT_TOPIC")
            ack.acknowledge()
        } catch (e: Exception) {
            log.error("Unexpected error when processing records in $KANDIDAT_TOPIC: ${e.message}", e)
        }
    }

    private fun processRecords(records: List<ConsumerRecord<String, KandidatKafkaEvent?>>) {
        val kandidater = records
            .mapNotNull { it.value()?.toKandidat() }
            .filter {
                when (it.status) {
                    KandidatStatus.IKKE_KANDIDAT -> {
                        log.info("Received kandidat with kandidatId: ${it.kandidatId} where status is IKKE_KANDIDAT. Skipping...")
                        return@filter false
                    }
                    KandidatStatus.KANDIDAT -> {
                        log.info("Storing kandidat for kandidatId: ${it.kandidatId}...")
                        return@filter true
                    }
                }
            }
        kandidatService.persistKandidater(kandidater)
        log.info("Persisted ${kandidater.size} kandidater")
    }

    companion object {
        const val KANDIDAT_TOPIC = "teamsykefravr.ismeroppfolging-kartleggingssporsmal-kandidat"
    }

    fun KandidatKafkaEvent.toKandidat() = KartleggingssporsmalKandidat(
        personIdent = this.personIdent,
        kandidatId = this.kandidatId,
        status = this.status,
        createdAt = this.createdAt.toInstant(),
    )
}
