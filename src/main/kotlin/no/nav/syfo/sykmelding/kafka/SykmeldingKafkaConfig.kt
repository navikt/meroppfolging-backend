package no.nav.syfo.sykmelding.kafka

import no.nav.syfo.config.kafka.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

@EnableKafka
@Configuration
class SykmeldingKafkaConfig(
    private val kafkaConfig: KafkaConfig,
) {
    @Bean
    fun sykmeldingConsumerFactory(): ConsumerFactory<String, String?> {
        val config = kafkaConfig.commonKafkaAivenConsumerConfig().toMutableMap().apply {
            put(ConsumerConfig.GROUP_ID_CONFIG, "meroppfolging-backend-ok-sykmelding-1")
            put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100")
        }
        return DefaultKafkaConsumerFactory(config)
    }

    @Bean
    fun sykmeldingKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String?> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String?>()
        factory.consumerFactory = sykmeldingConsumerFactory()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        factory.isBatchListener = true
        return factory
    }
}
