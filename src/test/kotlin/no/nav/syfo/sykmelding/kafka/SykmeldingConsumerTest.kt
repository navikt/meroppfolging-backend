package no.nav.syfo.sykmelding.kafka

import com.ninjasquad.springmockk.MockkBean
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.verify
import no.nav.syfo.LocalApplication
import no.nav.syfo.sykmelding.service.SykmeldingService
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistrar
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName
import kotlin.time.Duration.Companion.seconds

@SpringBootTest(classes = [LocalApplication::class])
@Import(SykmeldingConsumerTest.KafkaTestConfig::class)
@ApplyExtension(SpringExtension::class)
class SykmeldingConsumerTest : DescribeSpec() {

    @MockkBean(relaxed = true)
    private lateinit var sykmeldingService: SykmeldingService

    @TestConfiguration
    class KafkaTestConfig {
        companion object {
            val kafkaContainer: ConfluentKafkaContainer =
                ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:8.0.4"))
                    .apply { start() }
        }

        @Bean
        fun kafkaDynamicProperties(): DynamicPropertyRegistrar =
            DynamicPropertyRegistrar { registry ->
                registry.add("kafka.brokers") { kafkaContainer.bootstrapServers }
            }
    }

    init {
        describe("SykmeldingConsumer") {
            it("should call deleteSykmelding when a tombstone record is received") {
                val sykmeldingId = "test-sykmelding-tombstone-id"

                KafkaProducer<String, String?>(
                    mapOf(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to KafkaTestConfig.kafkaContainer.bootstrapServers,
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                    ),
                ).use { producer ->
                    producer.send(ProducerRecord(SYKMELDING_TOPIC, sykmeldingId, null)).get()
                }

                eventually(30.seconds) {
                    verify { sykmeldingService.deleteSykmelding(sykmeldingId) }
                }
            }
        }
    }
}
