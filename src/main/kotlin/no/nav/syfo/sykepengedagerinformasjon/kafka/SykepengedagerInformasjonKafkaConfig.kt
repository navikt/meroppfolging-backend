package no.nav.syfo.sykepengedagerinformasjon.kafka

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
class SykepengedagerInformasjonKafkaConfig(
    private val kafkaConfig: KafkaConfig,
) {
    @Bean
    fun sykepengeDagerConsumerFactory(): ConsumerFactory<String, String?> {
        val config = kafkaConfig.commonKafkaAivenConsumerConfig().toMutableMap().apply {
            put(ConsumerConfig.GROUP_ID_CONFIG, "meroppfolging-backend-sykepengedager-01")
            put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100")
        }

        return DefaultKafkaConsumerFactory(config)
    }

    @Bean
    fun sykepengedagerInformasjonListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String?> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String?>()
        factory.consumerFactory = sykepengeDagerConsumerFactory()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL
        return factory
    }
}
