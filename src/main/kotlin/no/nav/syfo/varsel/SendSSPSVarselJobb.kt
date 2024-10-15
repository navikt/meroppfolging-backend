package no.nav.syfo.varsel

import no.nav.syfo.leaderelection.LeaderElectionClient
import no.nav.syfo.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SendSSPSVarselJobb(
    private val varselService: VarselService,
    private val leaderElectionClient: LeaderElectionClient,
    @Value("\${toggle.ssps.varselutsending}") private var varselutsendingEnabledForEnvironment: Boolean,
) {
    private val log = logger()
    private val logName = "[${SendSSPSVarselJobb::class.simpleName}]"

    @Scheduled(cron = "\${send.varsel.job.cron}")
    fun sendVarsler(): Int {
        if (!varselutsendingEnabledForEnvironment || !leaderElectionClient.isPodLeader()) {
            return 0
        }

        log.info("$logName Starter jobb")

        var antallVarslerSendt = 0

        val varslerToSendToday = varselService.findMerOppfolgingVarselToBeSent()
        log.info("$logName Planlegger å sende ${varslerToSendToday.size} varsler totalt")

        varslerToSendToday.forEach {
            try {
                varselService.sendMerOppfolgingVarsel(it)
                antallVarslerSendt++
            } catch (e: RuntimeException) {
                log.error("$logName Feil i utsending av varsel. ${e.message}", e)
            }
        }

        log.info("$logName Sendte $antallVarslerSendt varsler")
        log.info("$logName Avslutter jobb")

        return antallVarslerSendt
    }
}
