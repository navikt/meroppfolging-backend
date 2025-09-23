package no.nav.syfo.kartlegging.kafka

import no.nav.syfo.logger
import no.nav.syfo.sykmelding.kafka.SYKMELDING_TOPIC
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class KandidatConsumer {
    private val log = logger()

    @KafkaListener(topics = [KANDIDAT_TOPIC], containerFactory = "kandidatKafkaListenerContainerFactory")
    fun listen(
        records: List<ConsumerRecord<String, KandidatEvent?>>,
        ack: Acknowledgment,
    ) {
        try {
            log.info("Received ${records.size} sykmeldinger from $SYKMELDING_TOPIC")
            records.forEach { record ->
                // TODO: Implement processing logic for kandidat records
            }
            log.info("Committing offset for sykmeldinger")
            ack.acknowledge()
        } catch (e: Exception) {
            log.error("Unexpected error: ${e.message}", e)
        }
    }

    companion object {
        // TODO: Update topic name when available
        const val KANDIDAT_TOPIC = "teamsykefravr.kandidat"
    }
}
