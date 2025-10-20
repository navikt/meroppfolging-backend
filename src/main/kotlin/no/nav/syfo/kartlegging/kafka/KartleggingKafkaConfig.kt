package no.nav.syfo.kartlegging.kafka

import no.nav.syfo.config.kafka.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff
import org.springframework.util.backoff.FixedBackOff.UNLIMITED_ATTEMPTS

@EnableKafka
@Configuration
class KartleggingKafkaConfig(
    private val kafkaConfig: KafkaConfig,
) {
    @Bean
    fun kartleggingSvarProducerFactory() =
        DefaultKafkaProducerFactory<String, KartleggingssvarEvent>(
            kafkaConfig.commonKafkaAivenProducerConfig(),
        )

    @Bean
    fun kartleggingSvarKafkaTemplate(producerFactory: ProducerFactory<String, KartleggingssvarEvent>) =
        KafkaTemplate(producerFactory)

    @Bean
    fun kandidatConsumerFactory(): ConsumerFactory<String, String?> {
        val config = kafkaConfig.commonKafkaAivenConsumerConfig().toMutableMap().apply {
            put(ConsumerConfig.GROUP_ID_CONFIG, "meroppfolging-backend-kartlegging-kandidat")
            put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100")
        }
        return DefaultKafkaConsumerFactory(config)
    }

    @Bean
    fun kandidatKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String?> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String?>()
        factory.consumerFactory = kandidatConsumerFactory()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        factory.isBatchListener = true
        // try again every 30 seconds indefinitely
        factory.setCommonErrorHandler(
            DefaultErrorHandler(
                FixedBackOff(30000L, UNLIMITED_ATTEMPTS)
            )
        )
        return factory
    }
}
