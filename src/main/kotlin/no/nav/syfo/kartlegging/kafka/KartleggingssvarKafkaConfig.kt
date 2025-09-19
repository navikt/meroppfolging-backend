package no.nav.syfo.kartlegging.kafka

import no.nav.syfo.config.kafka.KafkaConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@EnableKafka
@Configuration
class KartleggingssvarKafkaConfig(
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
}
