package no.nav.syfo.senoppfolging.kafka

import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.KafkaException
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ExecutionException

@Component
class SenOppfolgingVarselKafkaProducer(private val kafkaTemplate: KafkaTemplate<String, KSenOppfolgingVarselDTO>,) {
    fun publishVarsel(message: KSenOppfolgingVarselDTO) {
        try {
            log.info("SenOppfolgingSvarProducer: Publiserer sen-oppfolging-varsel")
            kafkaTemplate
                .send(
                    ProducerRecord(
                        SEN_OPPFOLGING_VARSEL_TOPIC,
                        UUID.randomUUID().toString(),
                        message,
                    ),
                ).get()
        } catch (e: ExecutionException) {
            log.error(
                "ExecutionException was thrown when attempting to " +
                    "publish message to $SEN_OPPFOLGING_VARSEL_TOPIC. ${e.message}",
            )
            throw e
        } catch (e: KafkaException) {
            log.error(
                "KafkaException was thrown when attempting to " +
                    "publish message to $SEN_OPPFOLGING_VARSEL_TOPIC. ${e.message}",
            )
            throw e
        }
    }

    companion object {
        const val SEN_OPPFOLGING_VARSEL_TOPIC = "team-esyfo.sen-oppfolging-varsel"
        private val log = LoggerFactory.getLogger(SenOppfolgingVarselKafkaProducer::class.java)
    }
}
