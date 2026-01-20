package no.nav.syfo.senoppfolging.kafka

import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionTypeV2
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionV2
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.CompletableFuture

class SenOppfolgingSvarKafkaProducerTest :
    DescribeSpec(
        {
            val kafkaTemplate = mockk<KafkaTemplate<String, KSenOppfolgingSvarDTO>>(relaxed = true)
            val producer = SenOppfolgingSvarKafkaProducer(kafkaTemplate)
            val ansattFnr = "12345678910"

            beforeTest {
                clearAllMocks()
            }

            describe("Publish reponse") {

                it("Should publish message on kafka") {
                    val response = KSenOppfolgingSvarDTO(
                        UUID.randomUUID(),
                        ansattFnr,
                        LocalDateTime.now(),
                        listOf(SenOppfolgingQuestionV2(SenOppfolgingQuestionTypeV2.BEHOV_FOR_OPPFOLGING, "", "", "")),
                        UUID.randomUUID(),
                    )

                    val sendResult = SendResult(
                        ProducerRecord("topic", "key", response),
                        RecordMetadata(
                            TopicPartition("topic", 0),
                            0,
                            0,
                            0,
                            0,
                            0
                        )
                    )
                    every {
                        kafkaTemplate.send(any<ProducerRecord<String, KSenOppfolgingSvarDTO>>())
                    } returns CompletableFuture.completedFuture(sendResult)

                    producer.publishResponse(response)
                    verify(atLeast = 1) {
                        kafkaTemplate.send(any<ProducerRecord<String, KSenOppfolgingSvarDTO>>())
                    }
                }
            }
        },
    )
