package no.nav.syfo.varsel

import no.nav.syfo.dokarkiv.DokarkivClient
import no.nav.syfo.logger
import no.nav.syfo.pdl.PdlClient
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
) {
    private val log = logger()
    fun findMerOppfolgingVarselToBeSent(): List<MerOppfolgingVarselDTO> {
        val allVarsler = varselRepository.fetchMerOppfolgingVarselToBeSent()

        val filteredVarsler = allVarsler.mapNotNull {
            val ageCheckResult = pdlClient.isBrukerYngreEnnGittMaxAlder(it.personIdent, 67)
            if (ageCheckResult.youngerThanMaxAlder) {
                it
            } else {
                varselRepository.storeSkipVarselDueToAge(it.personIdent, ageCheckResult.fodselsdato)
                null
            }
        }

        return filteredVarsler
    }

    fun ferdigstillMerOppfolgingVarsel(
        fnr: String,
    ) {
        val hendelse = ArbeidstakerHendelse(
            type = HendelseType.SM_MER_VEILEDNING,
            ferdigstill = true,
            data = null,
            arbeidstakerFnr = fnr,
            orgnummer = null,
        )
        producer.sendVarselTilEsyfovarsel(hendelse)
    }

    @Suppress("MaxLineLength")
    fun sendMerOppfolgingVarsel(
        merOppfolgingVarselDTO: MerOppfolgingVarselDTO,
    ) {
        val personIdent = merOppfolgingVarselDTO.personIdent
        try {
            val pdf = pdfgenService.getMerVeiledningLandingPdf(personIdent)
            val uuid = UUID.randomUUID().toString()

            val dokarkivResponse = dokarkivClient.postDocumentToDokarkiv(
                fnr = personIdent,
                pdf = pdf,
                uuid = uuid,
            )
            if (dokarkivResponse != null) {
                val hendelse = ArbeidstakerHendelse(
                    type = HendelseType.SM_MER_VEILEDNING,
                    ferdigstill = false,
                    data = VarselData(
                        VarselDataJournalpost(uuid = uuid, id = dokarkivResponse.journalpostId.toString()),
                        null,
                        null,
                        null,
                    ),
                    arbeidstakerFnr = personIdent,
                    orgnummer = null,
                )
                producer.sendVarselTilEsyfovarsel(hendelse)

                val utsendtVarselUUID = varselRepository.storeUtsendtVarsel(
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
                log.warn("Fetched journalpost id is null, skipped sending varsel")
            }
        } catch (e: Exception) {
            log.error(
                "Skipped sending varsel due to exception: ${e.cause}, ${e.message}",
            )
        }
    }

    fun getUtsendtVarsel(fnr: String): UtsendtVarsel? {
        return varselRepository.getUtsendtVarsel(fnr)
    }
}
