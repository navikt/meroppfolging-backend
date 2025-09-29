package no.nav.syfo.mikrofrontend.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.kartlegging.domain.PersistedKartleggingssporsmal
import no.nav.syfo.kartlegging.domain.KartleggingssporsmalKandidat
import no.nav.syfo.kartlegging.service.KandidatService
import no.nav.syfo.kartlegging.service.KartleggingssporsmalService
import no.nav.syfo.mikrofrontend.domain.KartleggingResponseStatusType
import no.nav.syfo.mikrofrontend.domain.MerOppfolgingStatusDTO
import no.nav.syfo.mikrofrontend.domain.OppfolgingsType
import no.nav.syfo.senoppfolging.service.SenOppfolgingService
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingStatusDTOV2
import java.time.Instant
import java.util.UUID

class MikrofrontendServiceTest : DescribeSpec({

    val senOppfolgingService = mockk<SenOppfolgingService>()
    val kartleggingssporsmalService = mockk<KartleggingssporsmalService>()
    val kandidatService = mockk<KandidatService>()
    val service = MikrofrontendService(
        senOppfolgingService = senOppfolgingService,
        kartleggingssporsmalService = kartleggingssporsmalService,
        kandidatService = kandidatService,
    )

    describe("MikrofrontendService.status") {

        it("returns SEN_OPPFOLGING when user has access") {
            val senStatus = mockk<SenOppfolgingStatusDTOV2>()
            every { senStatus.hasAccessToSenOppfolging } returns true
            every { senOppfolgingService.prepareAndBuildSenOppfolgingStatusDTOV2(any(), any()) } returns senStatus

            service.status("fnr", "token").oppfolgingsType shouldBe OppfolgingsType.SEN_OPPFOLGING
        }

        it("returns INGEN_OPPFOLGING when no sen access and not kandidat for kartlegging") {
            val senStatus = mockk<SenOppfolgingStatusDTOV2>()
            every { senStatus.hasAccessToSenOppfolging } returns false
            every { senOppfolgingService.prepareAndBuildSenOppfolgingStatusDTOV2(any(), any()) } returns senStatus
            every { kandidatService.getKandidatByFnr(any()) } returns null

            service.status("fnr", "token").oppfolgingsType shouldBe OppfolgingsType.INGEN_OPPFOLGING
        }

        it("returns KARTLEGGING with NO_RESPONSE when kandidat and no previous response") {
            val senStatus = mockk<SenOppfolgingStatusDTOV2>()
            every { senStatus.hasAccessToSenOppfolging } returns false
            every { senOppfolgingService.prepareAndBuildSenOppfolgingStatusDTOV2(any(), any()) } returns senStatus

            val testKandidatId = UUID.randomUUID()
            val kandidat = mockk<KartleggingssporsmalKandidat>()
            every { kandidat.kandidatId } returns testKandidatId
            every { kandidat.isKandidat() } returns true
            every { kandidatService.getKandidatByFnr(any()) } returns kandidat
            every { kartleggingssporsmalService.getLatestKartleggingssporsmal(testKandidatId) } returns null

            val result = service.status("fnr", "token") as MerOppfolgingStatusDTO.Kartlegging
            result.oppfolgingsType shouldBe OppfolgingsType.KARTLEGGING
            result.kartleggingStatus.responseStatus shouldBe KartleggingResponseStatusType.NO_RESPONSE
            result.kartleggingStatus.responseDateTime shouldBe null
            result.kartleggingStatus.hasAccessToKartlegging shouldBe true
        }

        it("returns KARTLEGGING with SUBMITTED when kandidat and response exists") {
            val senStatus = mockk<SenOppfolgingStatusDTOV2>()
            every { senStatus.hasAccessToSenOppfolging } returns false
            every { senOppfolgingService.prepareAndBuildSenOppfolgingStatusDTOV2(any(), any()) } returns senStatus

            val testKandidatId = UUID.randomUUID()
            val kandidat = mockk<KartleggingssporsmalKandidat>()
            every { kandidat.kandidatId } returns testKandidatId
            every { kandidat.isKandidat() } returns true
            every { kandidatService.getKandidatByFnr(any()) } returns kandidat

            val responseCreatedAt = Instant.parse("2024-05-01T10:15:30Z")
            val persisted = mockk<PersistedKartleggingssporsmal>()
            every { persisted.createdAt } returns responseCreatedAt
            every { kartleggingssporsmalService.getLatestKartleggingssporsmal(testKandidatId) } returns persisted

            val result = service.status("fnr", "token") as MerOppfolgingStatusDTO.Kartlegging
            result.oppfolgingsType shouldBe OppfolgingsType.KARTLEGGING
            result.kartleggingStatus.responseStatus shouldBe KartleggingResponseStatusType.SUBMITTED
            result.kartleggingStatus.responseDateTime shouldBe "2024-05-01T10:15:30Z"
            result.kartleggingStatus.hasAccessToKartlegging shouldBe true
        }
    }
})
