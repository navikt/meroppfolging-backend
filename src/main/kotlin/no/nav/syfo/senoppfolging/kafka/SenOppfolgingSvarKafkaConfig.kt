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
class SenOppfolgingSvarKafkaConfig(
    private val kafkaConfig: KafkaConfig,
) {
    @Bean
    fun senOppfolgingsvarProducerFactory(): ProducerFactory<String, KSenOppfolgingSvarDTOV2> =
        DefaultKafkaProducerFactory(kafkaConfig.commonKafkaAivenProducerConfig())

    @Bean
    fun senOppfolgingsvarKafkaTemplate(producerFactory: ProducerFactory<String, KSenOppfolgingSvarDTOV2>):
        KafkaTemplate<String, KSenOppfolgingSvarDTOV2> = KafkaTemplate(producerFactory)
}
