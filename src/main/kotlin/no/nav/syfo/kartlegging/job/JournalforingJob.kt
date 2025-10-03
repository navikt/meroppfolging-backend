package no.nav.syfo.kartlegging.job

import no.nav.syfo.kartlegging.service.KartleggingssporsmalService
import no.nav.syfo.leaderelection.LeaderElectionClient
import no.nav.syfo.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class JournalforingJob(
    private val leaderElectionClient: LeaderElectionClient,
    private val kartleggingssporsmalService: KartleggingssporsmalService,
) {
    private val logger = logger()
    private val logName = "[${JournalforingJob::class.simpleName}]"


    @Scheduled(cron = "\${retry.kartlegging.journalfor.job.cron}")
    fun journalforSvarForKartleggingssporsmal() {
        if (!leaderElectionClient.isPodLeader()) {
            return
        }
        logger.info("$logName Starter jobb for å journalføre svar kartleggingsspørsmål")

        val kartleggingssporsmalNotJournaled = kartleggingssporsmalService.getKartleggingssporsmalNotJournaled()
        logger.info("$logName Fant ${kartleggingssporsmalNotJournaled.size} kartleggingsspørsmål som ikke er journalført")
        kartleggingssporsmalNotJournaled.forEach {
            kartleggingssporsmalService.jornalforKartleggingssporsmal( it)
        }
        logger.info("$logName Avslutter jobb for å journalføre svar kartleggingsspørsmål")
    }
}
