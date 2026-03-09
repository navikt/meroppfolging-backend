package no.nav.syfo.sykepengedagerinformasjon.kafka

import no.nav.syfo.logger
import no.nav.syfo.sykepengedagerinformasjon.domain.SykepengedagerInformasjonDTO
import no.nav.syfo.sykepengedagerinformasjon.service.SykepengedagerInformasjonService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.readValue
import java.io.IOException

const val SPDI_TOPIC = "team-esyfo.sykepengedager-informasjon-topic"

@Component
class SykepengedagerInformasjonConsumer(private val sykepengeDagerService: SykepengedagerInformasjonService) {

    val log = logger()

    val objectMapper = jsonMapper {
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    @KafkaListener(topics = [SPDI_TOPIC], containerFactory = "sykepengedagerInformasjonListenerContainerFactory")
    fun listen(record: ConsumerRecord<String, String>, ack: Acknowledgment,) {
        try {
            log.info("[SYKEPENGEDAGER_INFO] received a record from topic")
            val sykepengedagerInformasjonDTO: SykepengedagerInformasjonDTO = objectMapper.readValue(record.value())
            sykepengeDagerService.processSykepengeDagerRecord(sykepengedagerInformasjonDTO)
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
