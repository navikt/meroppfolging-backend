package no.nav.syfo.kartlegging.domain

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.Instant
import java.util.UUID

class KartleggingssporsmalKandidatTest : DescribeSpec({
    describe("KartleggingssporsmalKandidat.isKandidat") {
        it("should return true when createdAt field is less than KANDIDAT_VALID_DAYS ago") {
            val kandidat = KartleggingssporsmalKandidat(
                personIdent = "12345678910",
                kandidatId = UUID.randomUUID(),
                status = KandidatStatus.KANDIDAT,
                createdAt = Instant.now().minus(Duration.ofDays(30)),
            )
            kandidat.isKandidat() shouldBe true
        }

        it("should return false when createdAt field is more than KANDIDAT_VALID_DAYS ago") {
            val kandidat = KartleggingssporsmalKandidat(
                personIdent = "12345678910",
                kandidatId = UUID.randomUUID(),
                status = KandidatStatus.KANDIDAT,
                createdAt = Instant.now().minus(Duration.ofDays(31)),
            )
            kandidat.isKandidat() shouldBe false
        }
    }
})
