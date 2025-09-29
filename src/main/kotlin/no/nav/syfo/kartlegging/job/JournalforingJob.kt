package no.nav.syfo.kartlegging.job

import no.nav.syfo.kartlegging.service.KartleggingssporsmalService
import no.nav.syfo.leaderelection.LeaderElectionClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class JournalforingJob(
    private val leaderElectionClient: LeaderElectionClient,
    private val kartleggingssporsmalService: KartleggingssporsmalService,
) {

    @Scheduled(cron = "\${retry.kartlegging.journalfor.job.cron}")
    fun retryJournalforing() {
        if (!leaderElectionClient.isPodLeader()) {
            return
        }

        val kartleggingssporsmalNotJournaled = kartleggingssporsmalService.getKartleggingssporsmalNotJournaled()
        kartleggingssporsmalNotJournaled.forEach {
            kartleggingssporsmalService.jornalforKartleggingssporsmal(it.uuid, it, it.createdAt)
        }
    }
}
