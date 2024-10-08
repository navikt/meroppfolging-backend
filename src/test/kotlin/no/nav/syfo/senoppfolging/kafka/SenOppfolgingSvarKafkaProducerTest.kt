package no.nav.syfo.senoppfolging.kafka

import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionTypeV2
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingQuestionV2
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.KafkaTemplate
import java.time.LocalDateTime
import java.util.*

class SenOppfolgingSvarKafkaProducerTest : DescribeSpec(
    {
        val kafkaTemplate = mockk<KafkaTemplate<String, KSenOppfolgingSvarDTO>>(relaxed = true)
        val producer = SenOppfolgingSvarKafkaProducer(kafkaTemplate)
        val ansattFnr = "12345678910"

        beforeTest {
            clearAllMocks()
        }

        describe("Publish reponse") {

            it("Should publish message on kafka") {
                producer.publishResponse(
                    KSenOppfolgingSvarDTO(
                        UUID.randomUUID(),
                        ansattFnr,
                        LocalDateTime.now(),
                        listOf(SenOppfolgingQuestionV2(SenOppfolgingQuestionTypeV2.BEHOV_FOR_OPPFOLGING, "", "", "")),
                        UUID.randomUUID(),
                    ),
                )

                verify(atLeast = 1) {
                    kafkaTemplate.send(any<ProducerRecord<String, KSenOppfolgingSvarDTO>>())
                }
            }
        }
    },
)
