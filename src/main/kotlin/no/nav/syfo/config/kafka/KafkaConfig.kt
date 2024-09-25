package no.nav.syfo.config.kafka

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.kafka.annotation.EnableKafka

@Configuration
@EnableKafka
class KafkaConfig(
    private val env: Environment,
    @Value("\${kafka.brokers}") private val aivenBrokers: String,
    @Value("\${kafka.truststore.path}") private val aivenTruststoreLocation: String,
    @Value("\${kafka.keystore.path}") private val aivenKeystoreLocation: String,
    @Value("\${kafka.credstore.password}") private val aivenCredstorePassword: String,
) {

    fun commonKafkaAivenConsumerConfig(): Map<String, Any> {
        return mapOf(
            ConsumerConfig.GROUP_ID_CONFIG to "meroppfolging-backend-01",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1",
            ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG to (10 * 1024 * 1024).toString(),
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to aivenBrokers,
        ) + securityConfig()
    }

    fun commonKafkaAivenProducerConfig(): Map<String, Any> {
        return mapOf(
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JacksonKafkaSerializer::class.java,
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to aivenBrokers,
        ) + securityConfig()
    }

    private fun securityConfig(): Map<String, Any> =
        if (isLocal) {
            mapOf()
        } else {
            mapOf<String, Any>(
                CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
                SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to aivenTruststoreLocation,
                SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to aivenCredstorePassword,
                SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "JKS",
                SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to aivenKeystoreLocation,
                SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to aivenCredstorePassword,
                SslConfigs.SSL_KEY_PASSWORD_CONFIG to aivenCredstorePassword,
                SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12",
            )
        }

    private val isLocal: Boolean
        get() = env.activeProfiles.any {
            it.equals("local", ignoreCase = true) ||
                it.equals("test", ignoreCase = true)
        }
}
