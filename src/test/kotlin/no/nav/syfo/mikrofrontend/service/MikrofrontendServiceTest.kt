package no.nav.syfo.mikrofrontend.service

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.mikrofrontend.domain.OppfolgingsType
import no.nav.syfo.senoppfolging.service.SenOppfolgingService
import no.nav.syfo.senoppfolging.v2.domain.SenOppfolgingStatusDTOV2

class MikrofrontendServiceTest : DescribeSpec({

    val senOppfolgingService = mockk<SenOppfolgingService>()
    val service = MikrofrontendService(senOppfolgingService)

    describe("MikrofrontendService.status") {

        it("returns oppfolgingsType SEN_OPPFOLGING when has access to sen oppfolging") {
            val senStatus = mockk<SenOppfolgingStatusDTOV2> {
                every { hasAccessToSenOppfolging } returns true
            }
            every {
                senOppfolgingService.prepareAndBuildSenOppfolgingStatusDTOV2(any(), any())
            } returns senStatus

            val result = service.status("11111111111", "token")

            result.oppfolgingsType shouldBe OppfolgingsType.SEN_OPPFOLGING
        }

        it("returns oppfolgingsType INGEN_OPPFOLGING when no access to sen oppfølging, and kartlegging flag is false") {
            val senStatus = mockk<SenOppfolgingStatusDTOV2> {
                every { hasAccessToSenOppfolging } returns false
            }
            every {
                senOppfolgingService.prepareAndBuildSenOppfolgingStatusDTOV2(any(), any())
            } returns senStatus

            val result = service.status("11111111111", "token")

            result.oppfolgingsType shouldBe OppfolgingsType.INGEN_OPPFOLGING
        }
    }
})
