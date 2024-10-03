package no.nav.syfo.scheduled

import no.nav.syfo.dokarkiv.DokarkivClient
import no.nav.syfo.dokarkiv.database.JournalforFailedDAO
import no.nav.syfo.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RetryJournalforing(
    private val journalforFailedDAO: JournalforFailedDAO,
    private val dokarkivClient: DokarkivClient,
) {
    private val logger = logger()

    @Scheduled(cron = "0 0/5 * * * *") // Runs at the top of every 5 minutes
    @Transactional(readOnly = true)
    fun fetchDataFromDatabase() {
        logger.info("Scheduled retry journalforing task started")
        try {
            val failedJournalpost = journalforFailedDAO.fetchJournalforFailed()
            failedJournalpost?.forEach { jp ->
                logger.info("Starting to resend failed journalpost with varselId ${jp.varselUuid}")
                dokarkivClient.postDocumentToDokarkiv(
                    fnr = jp.personIdent,
                    pdf = jp.pdf,
                    uuid = jp.varselUuid,
                )
            }
        } catch (e: Exception) {
            logger.error("Journalforing retry failed ", e)
        }

        logger.info("Retry journalforing task completed")
    }
}
