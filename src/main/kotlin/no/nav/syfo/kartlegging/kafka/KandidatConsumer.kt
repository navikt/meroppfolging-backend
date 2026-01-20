package no.nav.syfo.kartlegging.kafka

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.syfo.kartlegging.domain.KandidatStatus
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartlegging.service.KandidatService
import no.nav.syfo.logger
import no.nav.syfo.metric.Metric
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class KandidatConsumer(private val kandidatService: KandidatService, private val metric: Metric,) {
    private val log = logger()
    private val objectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    @KafkaListener(topics = [KANDIDAT_TOPIC], containerFactory = "kandidatKafkaListenerContainerFactory")
    fun listen(records: List<ConsumerRecord<String, String?>>, ack: Acknowledgment,) {
        try {
            log.info("Received ${records.size} kandidater from $KANDIDAT_TOPIC")
            processRecords(records)
            log.info("Committing offset for topic $KANDIDAT_TOPIC")
            ack.acknowledge()
        } catch (e: Exception) {
            log.error("Unexpected error when processing records in $KANDIDAT_TOPIC: ${e.message}", e)
            throw e
        }
    }

    private fun processRecords(records: List<ConsumerRecord<String, String?>>) {
        val kandidater = records
            .mapNotNull { it.value() }
            .mapNotNull {
                objectMapper.readValue<KandidatKafkaEvent>(it)
                    .toKandidat()
            }
            .filter {
                when (it.status) {
                    KandidatStatus.IKKE_KANDIDAT -> {
                        log.info(
                            "Received kandidat with kandidatId: ${it.kandidatId} where status is IKKE_KANDIDAT. Skipping..."
                        )
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

        metric.countKartleggingssporsmalKandidatReceived(kandidater.size.toDouble())
    }

    companion object {
        const val KANDIDAT_TOPIC = "teamsykefravr.ismeroppfolging-kartleggingssporsmal-kandidat"
    }

    fun KandidatKafkaEvent.toKandidat(): KartleggingssporsmalKandidat? {
        val status = try {
            KandidatStatus.valueOf(this.status)
        } catch (e: IllegalArgumentException) {
            log.warn("Ukjent kandidat status. Forkaster kandidat ${this.kandidatUuid}", e)
            return null
        }

        return KartleggingssporsmalKandidat(
            personIdent = this.personident,
            kandidatId = this.kandidatUuid,
            status = status,
            createdAt = this.createdAt.toInstant(),
        )
    }
}
