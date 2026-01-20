package no.nav.syfo.senoppfolging.kafka

import no.nav.syfo.config.kafka.KafkaConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@EnableKafka
@Configuration
class SenOppfolgingSvarKafkaConfig(private val kafkaConfig: KafkaConfig,) {
    @Bean
    fun senOppfolgingsvarProducerFactory(): ProducerFactory<String, KSenOppfolgingSvarDTO> =
        DefaultKafkaProducerFactory(kafkaConfig.commonKafkaAivenProducerConfig())

    @Bean
    fun senOppfolgingsvarKafkaTemplate(
        producerFactory: ProducerFactory<String, KSenOppfolgingSvarDTO>
    ): KafkaTemplate<String, KSenOppfolgingSvarDTO> = KafkaTemplate(producerFactory)
}
