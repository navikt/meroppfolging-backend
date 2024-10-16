package no.nav.syfo.varsel

import no.nav.syfo.leaderelection.LeaderElectionClient
import no.nav.syfo.logger
import no.nav.syfo.varsel.database.SendingDueDateDAO
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class SendSSPSVarselJobb(
    private val varselService: VarselService,
    private val leaderElectionClient: LeaderElectionClient,
    @Value("\${toggle.ssps.varselutsending}") private var varselutsendingEnabledForEnvironment: Boolean,
    private var sendingDueDateDAO: SendingDueDateDAO,
) {
    private val log = logger()
    private val logName = "[${SendSSPSVarselJobb::class.simpleName}]"

    @Scheduled(cron = "\${send.varsel.job.cron}")
    fun sendVarsler(): Int {
        if (!varselutsendingEnabledForEnvironment || !leaderElectionClient.isPodLeader()) {
            return 0
        }

        log.info("$logName Starter jobb")

        val varslerToSendToday = varselService.findMerOppfolgingVarselToBeSent()
        log.info("$logName Planlegger å sende ${varslerToSendToday.size} varsler totalt")

        val antallVarslerSendt = sendVarslerWithHandling(varslerToSendToday)

        log.info("$logName Sendte $antallVarslerSendt varsler")
        log.info("$logName Avslutter jobb")

        return antallVarslerSendt
    }

    private fun sendVarslerWithHandling(varsler: List<MerOppfolgingVarselDTO>): Int {
        var antallVarslerSendt = 0

        varsler.forEach { varsel ->
            try {
                sendVarsel(varsel)
                antallVarslerSendt++
            } catch (e: RuntimeException) {
                handleSendVarselError(varsel, e)
            }
        }

        return antallVarslerSendt
    }

    private fun sendVarsel(varsel: MerOppfolgingVarselDTO) {
        varselService.sendMerOppfolgingVarsel(varsel)
        sendingDueDateDAO.deleteSendingDueDate(varsel.sykmeldingId)
    }

    private fun handleSendVarselError(varsel: MerOppfolgingVarselDTO, e: RuntimeException) {
        log.error("$logName Feil i utsending av varsel. ${e.message}", e)

        val dueDate = sendingDueDateDAO.getSendingDueDate(varsel.personIdent, varsel.sykmeldingId, varsel.utbetalingId)

        if (dueDate == null) {
            sendingDueDateDAO.persistSendingDueDate(varsel)
        } else if (dueDate.isDueDatePassed()) {
            log.error("[TOO MANY DAYS]: SSPS varsel must be sent before $dueDate!")
        }
    }

    private fun LocalDateTime.isDueDatePassed(): Boolean {
        return this.isEqual(LocalDateTime.now())
    }
}
