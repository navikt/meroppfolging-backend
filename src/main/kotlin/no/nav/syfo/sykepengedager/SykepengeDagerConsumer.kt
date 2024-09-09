package no.nav.syfo.sykepengedager

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.syfo.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.io.IOException

const val SPDI_TOPIC = "TODO"

@Suppress("TooGenericExceptionCaught")
@Component
class SykepengedagerConsumer(private val sykepengeDagerService: SykepengeDagerService) {

    val log = logger()

    val objectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    //    @KafkaListener(topics = [SPDI_TOPIC], containerFactory = "sykepengeDagerKafkaListenerContainerFactory")
    fun listenToTopic(
        record: ConsumerRecord<String, String>,
        ack: Acknowledgment,
    ) {
        try {
            val sykepengeDagerDTO: SykepengeDagerDTO = objectMapper.readValue(record.value())
            sykepengeDagerService.processSykepengeDagerRecord(sykepengeDagerDTO)
            ack.acknowledge()
        } catch (e: IOException) {
            log.error(
                "IOException during record processing from topic $SPDI_TOPIC.",
                e,
            )
        } catch (e: IllegalArgumentException) {
            log.error(
                "IllegalArgumentException during record processing from topic $SPDI_TOPIC",
                e,
            )
        } catch (e: Exception) {
            log.error(
                "Exception during record processing from topic $SPDI_TOPIC",
                e,
            )
        }
    }
}
