package no.nav.syfo.kartlegging.kafka

import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KartleggingssvarKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, KartleggingssvarEvent>,
) {

    fun publishResponse(event: KartleggingssvarEvent) {
        logger.info("KartleggingssvarKafkaProducer: Publiserer kartleggingsvar")
        kafkaTemplate.send(
            ProducerRecord(
                KARTLEGGINGSVAR_TOPIC,
                event.svarId.toString(),
                event,
            ),
        ).whenComplete { _, throwable ->
            if (throwable != null) {
                logger.error("Noe gikk galt ved sending av melding til topic $KARTLEGGINGSVAR_TOPIC", throwable)
            } else {
                logger.info("Melding sendt til topic $KARTLEGGINGSVAR_TOPIC")
            }
        }
    }

    companion object {
        const val KARTLEGGINGSVAR_TOPIC = "team-esyfo.kartleggingssporsmal-svar"
        private val logger = LoggerFactory.getLogger(KartleggingssvarKafkaProducer::class.java)
    }
}
