package no.nav.syfo.sykmelding.kafka

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.syfo.logger
import no.nav.syfo.sykmelding.domain.ReceivedSykmeldingDTO
import no.nav.syfo.sykmelding.service.SykmeldingService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.io.IOException

const val SYKMELDING_TOPIC = "teamsykmelding.ok-sykmelding"

@Component
class SykmeldingConsumer(private val sykmeldingService: SykmeldingService,) {
    private val log = logger()
    private val objectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    @KafkaListener(topics = [SYKMELDING_TOPIC], containerFactory = "sykmeldingKafkaListenerContainerFactory")
    fun listen(records: List<ConsumerRecord<String, String?>>, ack: Acknowledgment,) {
        try {
            log.info("Received ${records.size} sykmeldinger from $SYKMELDING_TOPIC")
            records.forEach { record ->
                processRecord(record)
            }
            log.info("Committing offset for sykmeldinger")
            ack.acknowledge()
        } catch (e: JsonProcessingException) {
            log.error("JSON processing error: ${e.message}", e)
        } catch (e: IOException) {
            log.error("I/O error: ${e.message}", e)
        } catch (e: ProcessSykmeldingException) {
            log.error("Unexpected error: ${e.message}", e)
        }
    }

    private fun processRecord(record: ConsumerRecord<String, String?>) {
        val sykmeldingId = record.key()
        val sykmeldingKafkaMessage: ReceivedSykmeldingDTO? = record.value()?.let { objectMapper.readValue(it) }

        if (sykmeldingKafkaMessage == null) {
            log.info("Received tombstone record for sykmeldingId: $sykmeldingId ..deleting")
            sykmeldingService.deleteSykmelding(sykmeldingId)
        } else {
            log.info("Storing sykmeldingsperioder for sykmeldingId: $sykmeldingId")
            sykmeldingService.persistSykmelding(
                sykmeldingId = sykmeldingId,
                employeeIdentificationNumber = sykmeldingKafkaMessage.personNrPasient,
                sykmeldingsperioder = sykmeldingKafkaMessage.sykmelding.perioder,
            )
        }
    }
}
