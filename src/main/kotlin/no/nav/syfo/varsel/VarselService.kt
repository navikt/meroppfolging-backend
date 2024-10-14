package no.nav.syfo.varsel

import no.nav.syfo.behandlendeenhet.BehandlendeEnhetClient
import no.nav.syfo.behandlendeenhet.domain.isPilot
import no.nav.syfo.dokarkiv.DokarkivClient
import no.nav.syfo.logger
import no.nav.syfo.pdl.PdlClient
import no.nav.syfo.senoppfolging.kafka.KSenOppfolgingVarselDTO
import no.nav.syfo.senoppfolging.kafka.SenOppfolgingVarselKafkaProducer
import no.nav.syfo.syfoopppdfgen.PdfgenService
import org.springframework.beans.factory.annotation.Value
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
    private val behandlendeEnhetClient: BehandlendeEnhetClient,
    @Value("\${NAIS_CLUSTER_NAME}") private var clusterName: String,
) {
    private val log = logger()
    fun findMerOppfolgingVarselToBeSent(): List<MerOppfolgingVarselDTO> {
        return varselRepository.fetchMerOppfolgingVarselToBeSent()
            .filter {
                pdlClient.isBrukerYngreEnnGittMaxAlder(it.personIdent, 67)
            }
            .filter {
                val behandlendeEnhet = behandlendeEnhetClient.getBehandlendeEnhet(it.personIdent)
                !behandlendeEnhet.isPilot(clusterName)
            }
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
            val pdf = pdfgenService.getMerVeiledningPdf(personIdent)

            val dokarkivResponse = dokarkivClient.postDocumentToDokarkiv(
                fnr = personIdent,
                pdf = pdf,
                uuid = UUID.randomUUID().toString(),
            )
            if (dokarkivResponse != null) {
                val hendelse = ArbeidstakerHendelse(
                    type = HendelseType.SM_MER_VEILEDNING,
                    ferdigstill = false,
                    data = VarselData(
                        VarselDataJournalpost(dokarkivResponse.journalpostId.toString(), null),
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
