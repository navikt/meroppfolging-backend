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
class SenOppfolgingVarselKafkaConfig(
    private val kafkaConfig: KafkaConfig,
) {
    @Bean
    fun senOppfolgingVarselProducerFactory(): ProducerFactory<String, KSenOppfolgingVarselDTO> =
        DefaultKafkaProducerFactory(kafkaConfig.commonKafkaAivenProducerConfig())

    @Bean
    fun senOppfolgingVarselKafkaTemplate(
        producerFactory: ProducerFactory<String, KSenOppfolgingVarselDTO>,
    ): KafkaTemplate<String, KSenOppfolgingVarselDTO> = KafkaTemplate(producerFactory)
}
