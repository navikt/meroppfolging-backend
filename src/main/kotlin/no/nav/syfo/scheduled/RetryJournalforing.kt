package no.nav.syfo.scheduled

import no.nav.syfo.dokarkiv.DokarkivClient
import no.nav.syfo.dokarkiv.database.JournalforFailedDAO
import no.nav.syfo.logger
import no.nav.syfo.syfoopppdfgen.PdfgenService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RetryJournalforing(
    private val journalforFailedDAO: JournalforFailedDAO,
    private val dokarkivClient: DokarkivClient,
    private val pdfgenService: PdfgenService,
) {
    private val logger = logger()

    @Scheduled(cron = "0 0/5 * * * *") // Runs at the top of every 5 minutes
    @Transactional(readOnly = true)
    fun fetchDataFromDatabase() {
        logger.info("Scheduled retry journalforing task started")
        try {
            val failedJournalpost = journalforFailedDAO.fetchJournalforingFailed()
            failedJournalpost?.forEach { jp ->
                logger.info("Starting to resend failed journalpost with varselId ${jp.varselUuid}")
                val pdf: ByteArray? = jp.pdf

                if (pdf !== null) {
                    dokarkivClient.postDocumentToDokarkiv(
                        fnr = jp.personIdent,
                        pdf = pdf,
                        uuid = jp.varselUuid,
                    )
                } else {
                    val pdfRetry = pdfgenService.getMerVeiledningPdf()
                    if (pdfRetry !== null) {
                        dokarkivClient.postDocumentToDokarkiv(
                            fnr = jp.personIdent,
                            pdf = pdfRetry,
                            uuid = jp.varselUuid,
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Journalforing retry failed ", e)
        }

        logger.info("Retry journalforing task completed")
    }
}
