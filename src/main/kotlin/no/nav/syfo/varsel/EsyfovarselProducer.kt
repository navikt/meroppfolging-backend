package no.nav.syfo.varsel

import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.KafkaException
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ExecutionException

@Component
class EsyfovarselProducer(
    private val kafkaTemplate: KafkaTemplate<String, EsyfovarselHendelse>,
) {
    fun sendVarselTilEsyfovarsel(
        hendelse: EsyfovarselHendelse,
    ) {
        try {
            val handling = if (hendelse.ferdigstill == true) "Ferdigstiller" else "Sender"
            log.info("EsyfovarselProducer: $handling varsel av type ${hendelse.type.name}")
            kafkaTemplate.send(
                ProducerRecord(
                    ESYFOVARSEL_TOPIC,
                    UUID.randomUUID().toString(),
                    hendelse,
                ),
            ).get()
        } catch (e: ExecutionException) {
            log.error("ExecutionException was thrown when attempting to send varsel to esyfovarsel. ${e.message}")
            throw e
        } catch (e: KafkaException) {
            log.error("KafkaException was thrown when attempting to send varsel to esyfovarsel. ${e.message}")
            throw e
        }
    }

    companion object {
        const val ESYFOVARSEL_TOPIC = "team-esyfo.varselbus"
        private val log = LoggerFactory.getLogger(EsyfovarselProducer::class.java)
    }
}
