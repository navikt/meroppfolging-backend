package no.nav.syfo.varsel

import no.nav.syfo.config.kafka.KafkaConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@EnableKafka
@Configuration
class EsyfovarselKafkaConfig(
    private val kafkaConfig: KafkaConfig,
) {
    @Bean
    fun esyfovarselProducerFactory(): ProducerFactory<String, EsyfovarselHendelse> =
        DefaultKafkaProducerFactory(kafkaConfig.commonKafkaAivenProducerConfig())

    @Bean
    fun esyfovarselKafkaTemplate(producerFactory: ProducerFactory<String, EsyfovarselHendelse>): KafkaTemplate<String, EsyfovarselHendelse> =
        KafkaTemplate(producerFactory)
}
