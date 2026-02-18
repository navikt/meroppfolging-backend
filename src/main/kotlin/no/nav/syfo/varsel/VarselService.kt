package no.nav.syfo.varsel

import no.nav.syfo.dkif.DkifClient
import no.nav.syfo.dokarkiv.DokarkivClient
import no.nav.syfo.dokarkiv.domain.Distribusjonskanal
import no.nav.syfo.logger
import no.nav.syfo.metric.Metric
import no.nav.syfo.pdl.PdlClient
import no.nav.syfo.pdl.PdlClient.Personstatus.DOED
import no.nav.syfo.pdl.PdlClient.Personstatus.LEVENDE
import no.nav.syfo.pdl.PdlClient.Personstatus.UKJENT
import no.nav.syfo.senoppfolging.kafka.KSenOppfolgingVarselDTO
import no.nav.syfo.senoppfolging.kafka.SenOppfolgingVarselKafkaProducer
import no.nav.syfo.syfoopppdfgen.PdfgenService
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class VarselService(
    private val producer: EsyfovarselProducer,
    private val varselRepository: VarselRepository,
    private val pdlClient: PdlClient,
    private val senOppfolgingVarselKafkaProducer: SenOppfolgingVarselKafkaProducer,
    private val pdfgenService: PdfgenService,
    private val dokarkivClient: DokarkivClient,
    private val metric: Metric,
    private val dkifClient: DkifClient,
) {
    private val log = logger()
    fun findMerOppfolgingVarselToBeSent(): List<MerOppfolgingVarselDTO> {
        val allVarsler = varselRepository.fetchMerOppfolgingVarselToBeSent()

        val filteredVarsler = allVarsler.mapNotNull {
            val personstatus = pdlClient.hentPersonstatus(it.personIdent, 67)
            when (personstatus.status) {
                DOED -> {
                    log.info("Skipper varsel for person som er registrert doed i PDL")
                    varselRepository.storeSkipVarsel(it.personIdent, personstatus.fodselsdato, VarselSkipReason.DECEASED)
                    null
                }

                UKJENT -> {
                    log.warn("Skipper varsel fordi personstatus er ukjent fra PDL. Forsøker igjen i neste kjøring")
                    metric.countSenOppfolgingVarselSkippedDueToPdlUnknown()
                    null
                }

                LEVENDE -> {
                    if (personstatus.erUnderMaksAlder == false) {
                        varselRepository.storeSkipVarsel(it.personIdent, personstatus.fodselsdato, VarselSkipReason.AGE)
                        null
                    } else if (personstatus.erUnderMaksAlder == null) {
                        log.warn("Skipper varsel fordi aldersstatus er ukjent fra PDL. Forsøker igjen i neste kjøring")
                        metric.countSenOppfolgingVarselSkippedDueToPdlUnknown()
                        null
                    } else {
                        it
                    }
                }
            }
        }

        return filteredVarsler
    }

    fun ferdigstillMerOppfolgingVarsel(fnr: String,) {
        val hendelse = ArbeidstakerHendelse(
            type = HendelseType.SM_MER_VEILEDNING,
            ferdigstill = true,
            data = null,
            arbeidstakerFnr = fnr,
            orgnummer = null,
        )
        metric.countSenOppfolgingFerdigstilt()
        producer.sendVarselTilEsyfovarsel(hendelse)
    }

    @Suppress("MaxLineLength")
    fun sendMerOppfolgingVarsel(merOppfolgingVarselDTO: MerOppfolgingVarselDTO) {
        val personIdent = merOppfolgingVarselDTO.personIdent
        val isUserReservert = dkifClient.person(personIdent).kanVarsles == false

        try {
            val pdf = pdfgenService.getSenOppfolgingLandingPdf(personIdent, isUserReservert)
            val uuid = UUID.randomUUID().toString()

            val dokarkivResponse =
                dokarkivClient.postSingleDocumentToDokarkiv(
                    fnr = personIdent,
                    pdf = pdf,
                    eksternReferanseId = uuid,
                    title = "Snart slutt på sykepenger – Informasjon om maksdato og spørreskjema",
                    filnavn = "SSPS-informasjon",
                    kanal = if (!isUserReservert) Distribusjonskanal.NAV_NO else null,
                )
            if (dokarkivResponse != null) {
                val hendelse =
                    ArbeidstakerHendelse(
                        type = HendelseType.SM_MER_VEILEDNING,
                        ferdigstill = false,
                        data =
                        VarselData(
                            VarselDataJournalpost(uuid = uuid, id = dokarkivResponse.journalpostId),
                            null,
                            null,
                            null,
                        ),
                        arbeidstakerFnr = personIdent,
                        orgnummer = null,
                    )
                producer.sendVarselTilEsyfovarsel(hendelse)

                val utsendtVarselUUID =
                    varselRepository.storeUtsendtVarsel(
                        personIdent = merOppfolgingVarselDTO.personIdent,
                        utbetalingId = merOppfolgingVarselDTO.utbetalingId,
                        sykmeldingId = merOppfolgingVarselDTO.sykmeldingId,
                    )
                senOppfolgingVarselKafkaProducer.publishVarsel(
                    KSenOppfolgingVarselDTO(
                        uuid = utsendtVarselUUID,
                        personident = merOppfolgingVarselDTO.personIdent,
                        createdAt = LocalDateTime.now(),
                    ),
                )
            } else {
                log.error("Skipped sending varsel due to no DokarkivResponse")
            }
        } catch (e: Exception) {
            log.error(
                "Skipped sending varsel due to exception: ${e.cause}, ${e.message}",
            )
        }
    }

    fun getUtsendtVarsel(fnr: String): Varsel? {
        val utsendtVarsel = varselRepository.getUtsendtVarsel(fnr)
        val utsendtVarselEsyfovarselCopy = varselRepository.getUtsendtVarselFromEsyfovarselCopy(fnr)

        val validLatestUtsendtVarsel =
            listOfNotNull(
                utsendtVarsel,
                utsendtVarselEsyfovarselCopy,
            ).maxByOrNull { it.utsendtTidspunkt }

        return validLatestUtsendtVarsel
    }
}
