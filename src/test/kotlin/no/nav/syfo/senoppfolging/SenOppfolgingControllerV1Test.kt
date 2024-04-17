package no.nav.syfo.senoppfolging

import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.auth.TokenValidator
import no.nav.syfo.auth.getFnr
import no.nav.syfo.besvarelse.database.ResponseDao
import no.nav.syfo.metric.Metric
import no.nav.syfo.oppfolgingstilfelle.IsOppfolgingstilfelleClient
import no.nav.syfo.senoppfolging.domain.AndreForholdSvar
import no.nav.syfo.senoppfolging.domain.Besvarelse
import no.nav.syfo.senoppfolging.domain.FremtidigSituasjonSvar
import no.nav.syfo.senoppfolging.domain.SenOppfolgingDTOV1
import no.nav.syfo.senoppfolging.domain.SenOppfolgingQuestionTypeV1
import no.nav.syfo.senoppfolging.domain.SenOppfolgingQuestionV1
import no.nav.syfo.senoppfolging.domain.SenOppfolgingRegistrering
import no.nav.syfo.senoppfolging.domain.SisteStillingSvar
import no.nav.syfo.senoppfolging.domain.TekstForSporsmal
import no.nav.syfo.senoppfolging.domain.TilbakeIArbeidSvar
import no.nav.syfo.senoppfolging.domain.UtdanningBestattSvar
import no.nav.syfo.senoppfolging.domain.UtdanningGodkjentSvar
import no.nav.syfo.senoppfolging.domain.UtdanningSvar
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
        val responseDao = mockk<ResponseDao>(relaxed = true)

        val controller =
            SenOppfolgingControllerV1(
                "merOppfolgingFrontendClientId",
                mockk(relaxed = true),
                veilarbregistreringClient,
                isOppfolgingstilfelleClient,
                varselService,
                metric,
                responseDao,
            ).apply {
                this.tokenValidator = tokenValidator
            }

        val senOppfolgingRegistrering =
            SenOppfolgingRegistrering(
                besvarelse =
                Besvarelse(
                    utdanning = UtdanningSvar.HOYERE_UTDANNING_1_TIL_4,
                    utdanningBestatt = UtdanningBestattSvar.JA,
                    utdanningGodkjent = UtdanningGodkjentSvar.JA,
                    andreForhold = AndreForholdSvar.NEI,
                    sisteStilling = SisteStillingSvar.INGEN_SVAR,
                    fremtidigSituasjon = FremtidigSituasjonSvar.NY_ARBEIDSGIVER,
                    tilbakeIArbeid = TilbakeIArbeidSvar.NEI,
                ),
                teksterForBesvarelse =
                listOf(
                    TekstForSporsmal(
                        "utdanning",
                        "Hva er din høyeste fullførte utdanning?",
                        "Høyere utdanning (1 til 4 år)",
                    ),
                    TekstForSporsmal(
                        "utdanningBestatt",
                        "Er utdanningen din bestått?",
                        "Ja",
                    ),
                    TekstForSporsmal(
                        "utdanningGodkjent",
                        "Er utdanningen din godkjent i Norge?",
                        "Ja",
                    ),
                    TekstForSporsmal(
                        "andreForhold",
                        "Er det noe annet enn helsen din som NAV bør ta hensyn til?",
                        "Nei",
                    ),
                    TekstForSporsmal(
                        "sisteStilling",
                        "-",
                        "-",
                    ),
                    TekstForSporsmal(
                        "fremtidigSituasjon",
                        "Hva tenker du om din fremtidige situasjon?",
                        "Jeg trenger ny jobb",
                    ),
                    TekstForSporsmal(
                        "tilbakeIArbeid",
                        "Tror du at du kommer tilbake i jobb før du har vært sykmeldt i 52 uker?",
                        "Ingen svar",
                    ),
                ),
            )

        beforeTest {
            clearAllMocks()
        }

        describe("Submits form") {
            val ansattFnr = "12345678910"

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

            it("submit with need for oppfolging") {
                every { tokenValidator.validateTokenXClaims().getFnr() } returns ansattFnr
                controller.submitForm(
                    SenOppfolgingDTOV1(
                        senOppfolgingRegistrering,
                        listOf(
                            SenOppfolgingQuestionV1(SenOppfolgingQuestionTypeV1.ONSKER_OPPFOLGING, "Hei", "JA", "Ja"),
                        )
                    )
                )
                verify(exactly = 1) {
                    veilarbregistreringClient.completeRegistration(any(), senOppfolgingRegistrering)
                }
            }

            it("submit without need for oppfolging") {
                every { tokenValidator.validateTokenXClaims().getFnr() } returns ansattFnr
                controller.submitForm(
                    SenOppfolgingDTOV1(
                        senOppfolgingRegistrering,
                        listOf(
                            SenOppfolgingQuestionV1(SenOppfolgingQuestionTypeV1.ONSKER_OPPFOLGING, "Hei", "NEI", "Nei"),
                        )
                    )
                )
                verify(exactly = 0) {
                    veilarbregistreringClient.completeRegistration(any(), any())
                }
            }
        }
    },
)
