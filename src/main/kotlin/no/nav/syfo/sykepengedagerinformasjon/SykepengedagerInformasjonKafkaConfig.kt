package no.nav.syfo.sykepengedagerinformasjon

import no.nav.syfo.config.kafka.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory

@EnableKafka
@Configuration
class SykepengedagerInformasjonKafkaConfig(
    private val kafkaConfig: KafkaConfig,
) {
    @Bean
    fun sykepengeDagerConsumerFactory(): ConsumerFactory<String, String> {
        val config = kafkaConfig.commonKafkaAivenConsumerConfig()
        config[ConsumerConfig.GROUP_ID_CONFIG] = "meroppfolging-backend-sykepengedager-01"
        return DefaultKafkaConsumerFactory(config)
    }

    @Bean
    fun sykepengedagerInformasjonKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = sykepengeDagerConsumerFactory()
        return factory
    }
}
