package no.nav.syfo.kartlegging.kafka

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.syfo.kartlegging.domain.KandidatStatus
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartlegging.service.KandidatService
import no.nav.syfo.metric.Metric
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.support.Acknowledgment
import tools.jackson.module.kotlin.jsonMapper
import java.time.OffsetDateTime
import java.util.UUID

class KandidatConsumerTest : DescribeSpec({
    val kandidatService = mockk<KandidatService>()
    val metric = mockk<Metric>()
    val ack = mockk<Acknowledgment>()
    val consumer = KandidatConsumer(kandidatService, metric)
    val objectMapper = jsonMapper {}

    beforeTest {
        clearAllMocks()
        every { kandidatService.persistKandidater(any()) } just runs
        every { metric.countKartleggingssporsmalKandidatReceived(any()) } just runs
        every { ack.acknowledge() } just runs
    }

    fun createRecord(event: KandidatKafkaEvent): ConsumerRecord<String, String?> =
        ConsumerRecord(
            KandidatConsumer.KANDIDAT_TOPIC,
            0,
            0L,
            event.kandidatUuid.toString(),
            objectMapper.writeValueAsString(event),
        )

    fun createEvent(
        status: String = "KANDIDAT",
        skjemavariant: String? = null,
    ) = KandidatKafkaEvent(
        personident = "12345678910",
        kandidatUuid = UUID.randomUUID(),
        status = status,
        skjemavariant = skjemavariant,
        createdAt = OffsetDateTime.now(),
    )

    describe("listen") {
        it("should persist kandidat with status KANDIDAT and acknowledge") {
            val event = createEvent(status = "KANDIDAT", skjemavariant = "FLERVALG_FRITEKST_V1")
            val records = listOf(createRecord(event))

            consumer.listen(records, ack)

            val persisted = slot<List<KartleggingssporsmalKandidat>>()
            verify(exactly = 1) { kandidatService.persistKandidater(capture(persisted)) }
            persisted.captured.size shouldBe 1
            persisted.captured.first().personIdent shouldBe event.personident
            persisted.captured.first().kandidatId shouldBe event.kandidatUuid
            persisted.captured.first().status shouldBe KandidatStatus.KANDIDAT
            persisted.captured.first().skjemavariant shouldBe "FLERVALG_FRITEKST_V1"
            verify(exactly = 1) { metric.countKartleggingssporsmalKandidatReceived(1.0) }
            verify(exactly = 1) { ack.acknowledge() }
        }

        it("should filter out kandidat with status IKKE_KANDIDAT") {
            val event = createEvent(status = "IKKE_KANDIDAT")
            val records = listOf(createRecord(event))

            consumer.listen(records, ack)

            val persisted = slot<List<KartleggingssporsmalKandidat>>()
            verify(exactly = 1) { kandidatService.persistKandidater(capture(persisted)) }
            persisted.captured.size shouldBe 0
            verify(exactly = 1) { metric.countKartleggingssporsmalKandidatReceived(0.0) }
            verify(exactly = 1) { ack.acknowledge() }
        }

        it("should discard event with unknown status") {
            val event = createEvent(status = "UNKNOWN_STATUS")
            val records = listOf(createRecord(event))

            consumer.listen(records, ack)

            val persisted = slot<List<KartleggingssporsmalKandidat>>()
            verify(exactly = 1) { kandidatService.persistKandidater(capture(persisted)) }
            persisted.captured.size shouldBe 0
            verify(exactly = 1) { ack.acknowledge() }
        }

        it("should default skjemavariant to FLERVALG_V1 when null") {
            val event = createEvent(status = "KANDIDAT", skjemavariant = null)
            val records = listOf(createRecord(event))

            consumer.listen(records, ack)

            val persisted = slot<List<KartleggingssporsmalKandidat>>()
            verify(exactly = 1) { kandidatService.persistKandidater(capture(persisted)) }
            persisted.captured.first().skjemavariant shouldBe "FLERVALG_V1"
        }

        it("should skip tombstone records (null value)") {
            val record = ConsumerRecord<String, String?>(
                KandidatConsumer.KANDIDAT_TOPIC,
                0,
                0L,
                "some-key",
                null,
            )

            consumer.listen(listOf(record), ack)

            val persisted = slot<List<KartleggingssporsmalKandidat>>()
            verify(exactly = 1) { kandidatService.persistKandidater(capture(persisted)) }
            persisted.captured.size shouldBe 0
            verify(exactly = 1) { ack.acknowledge() }
        }

        it("should process multiple records and filter correctly") {
            val kandidat = createEvent(status = "KANDIDAT", skjemavariant = "FLERVALG_V1")
            val ikkeKandidat = createEvent(status = "IKKE_KANDIDAT")
            val unknown = createEvent(status = "BOGUS")
            val records = listOf(createRecord(kandidat), createRecord(ikkeKandidat), createRecord(unknown))

            consumer.listen(records, ack)

            val persisted = slot<List<KartleggingssporsmalKandidat>>()
            verify(exactly = 1) { kandidatService.persistKandidater(capture(persisted)) }
            persisted.captured.size shouldBe 1
            persisted.captured.first().kandidatId shouldBe kandidat.kandidatUuid
            verify(exactly = 1) { metric.countKartleggingssporsmalKandidatReceived(1.0) }
        }

        it("should rethrow exception and not acknowledge on failure") {
            val event = createEvent(status = "KANDIDAT")
            val records = listOf(createRecord(event))
            every { kandidatService.persistKandidater(any()) } throws RuntimeException("DB error")

            shouldThrow<RuntimeException> {
                consumer.listen(records, ack)
            }

            verify(exactly = 0) { ack.acknowledge() }
        }
    }
})
