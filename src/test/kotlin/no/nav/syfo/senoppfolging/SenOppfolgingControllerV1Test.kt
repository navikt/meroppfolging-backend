package no.nav.syfo.senoppfolging

import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.auth.getFnr
import no.nav.syfo.metric.Metric
import no.nav.syfo.oppfolgingstilfelle.IsOppfolgingstilfelleClient
import no.nav.syfo.varsel.ArbeidstakerHendelse
import no.nav.syfo.varsel.EsyfovarselProducer
import no.nav.syfo.varsel.HendelseType
import no.nav.syfo.varsel.VarselService
import no.nav.syfo.veilarbregistrering.VeilarbregistreringClient
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

class SenOppfolgingControllerV1Test : DescribeSpec(
    {
        val veilarbregistreringClient = mockk<VeilarbregistreringClient>(relaxed = true)
        val isOppfolgingstilfelleClient = mockk<IsOppfolgingstilfelleClient>(relaxed = true)
        val esyfovarselProducer = mockk<EsyfovarselProducer>(relaxed = true)
        val tokenValidator = mockk<TokenValidator>(relaxed = true)
        val metric = mockk<Metric>(relaxed = true)
        val varselService = VarselService(esyfovarselProducer)

        val controller = SenOppfolgingControllerV1(
            "merOppfolgingFrontendClientId",
            mockk(relaxed = true),
            veilarbregistreringClient,
            isOppfolgingstilfelleClient,
            varselService,
            metric,
        ).apply {
            this.tokenValidator = tokenValidator
        }

        beforeTest {
            clearAllMocks()
        }

        describe("Submits form") {
            val ansattFnr = "123456789"

            it("Should ferdigstill SM_MER_VEILEDNING varsel when visiting page") {
                every { tokenValidator.validateTokenXClaims().getFnr() } returns ansattFnr

                val request = MockHttpServletRequest()
                RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

                controller.visit()

                verify(exactly = 1) {
                    esyfovarselProducer.sendVarselTilEsyfovarsel(
                        match {
                            it is ArbeidstakerHendelse && it.arbeidstakerFnr == ansattFnr &&
                                    it.type == HendelseType.SM_MER_VEILEDNING &&
                                    it.ferdigstill == true
                        },
                    )
                }
            }
        }
    },
)
