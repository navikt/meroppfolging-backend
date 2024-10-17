package no.nav.syfo.senoppfolging.kafka

import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.KafkaException
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ExecutionException

@Component
class SenOppfolgingSvarKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, KSenOppfolgingSvarDTO>,
) {
    fun publishResponse(hendelse: KSenOppfolgingSvarDTO) {
        try {
            log.info("SenOppfolgingSvarProducer: Publiserer sen-oppfolging-svar")
            kafkaTemplate
                .send(
                    ProducerRecord(
                        SEN_OPPFOLGING_SVAR_TOPIC,
                        UUID.randomUUID().toString(),
                        hendelse,
                    ),
                ).get()
        } catch (e: ExecutionException) {
            log.error(
                "ExecutionException was thrown when attempting to " +
                    "publish response to $SEN_OPPFOLGING_SVAR_TOPIC. ${e.message}",
            )
            throw e
        } catch (e: KafkaException) {
            log.error(
                "KafkaException was thrown when attempting to " +
                    "publish response to $SEN_OPPFOLGING_SVAR_TOPIC. ${e.message}",
            )
            throw e
        }
    }

    companion object {
        const val SEN_OPPFOLGING_SVAR_TOPIC = "team-esyfo.sen-oppfolging-svar"
        private val log = LoggerFactory.getLogger(SenOppfolgingSvarKafkaProducer::class.java)
    }
}
