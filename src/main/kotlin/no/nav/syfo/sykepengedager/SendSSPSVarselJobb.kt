// package no.nav.syfo.sykepengedager
//
// import no.nav.syfo.client.pdl.PdlClient
// import no.nav.syfo.varsel.VarselService
// import org.slf4j.LoggerFactory
// import java.time.LocalDate
//
// class SendSSPSVarselJobb(
//    val sykepengeDagerService: SykepengeDagerService,
//    val pdlClient: PdlClient,
//    val varselService: VarselService,
// ) {
//    private val log = LoggerFactory.getLogger(SendSSPSVarselJobb::class.qualifiedName)
//    private val logName = "[${SendSSPSVarselJobb::class.simpleName}]"
//    val gjenstaendeSykedagerLimit = 91
//    val maxDateLimit = 13L
//
//    fun shouldSendVarsel(sykepengeDagerDTO: SykepengeDagerDTO): Boolean {
//        val hasLimitedRemainingSykedager: Boolean = sykepengeDagerDTO.gjenstaendeSykedager < gjenstaendeSykedagerLimit
//        val maxDateIsEqualToOrMoreThanTwoWeeksAway: Boolean =
//            sykepengeDagerDTO.maksDato.isAfter(LocalDate.now().plusDays(maxDateLimit))
//        val hasAlreadySentVarsel: Boolean = varselService.getUtsendtVarsel(sykepengeDagerDTO.fnr) != null
//        val isBrukerYngreEnn67Ar: Boolean = pdlClient.isBrukerYngreEnnGittMaxAlder(sykepengeDagerDTO.fnr, 67)
//        val hasActiveSykmelding = true // implement this
//
//        return hasLimitedRemainingSykedager &&
//                maxDateIsEqualToOrMoreThanTwoWeeksAway &&
//                !hasAlreadySentVarsel &&
//                isBrukerYngreEnn67Ar
//    }
//
//    fun sendVarsler(): Int {
//        log.info("$logName Starter jobb")
//
//        var antallVarslerSendt = 0
//
//        val varslerToSendToday = merVeiledningVarselFinder.findMerVeiledningVarslerToSendToday()
//
//        log.info("$logName Planlegger å sende ${varslerToSendToday.size} varsler totalt")
//
//        varslerToSendToday.forEach {
//            try {
//                merVeiledningVarselService.sendVarselTilArbeidstakerFromJob(
//                    ArbeidstakerHendelse(
//                        type = HendelseType.SM_MER_VEILEDNING,
//                        ferdigstill = false,
//                        data = null,
//                        arbeidstakerFnr = it.fnr,
//                        orgnummer = null,
//                    ),
//                    it.uuid,
//                )
//
//                mikrofrontendService.updateMikrofrontendForUserByHendelse(
//                    ArbeidstakerHendelse(
//                        type = HendelseType.SM_MER_VEILEDNING,
//                        ferdigstill = false,
//                        data = null,
//                        arbeidstakerFnr = it.fnr,
//                        orgnummer = null,
//                    ),
//                )
//
//                antallVarslerSendt++
//                log.info("$logName Sendt varsel med UUID ${it.uuid}")
//            } catch (e: RuntimeException) {
//                log.error("$logName Feil i utsending av varsel med UUID: ${it.uuid} | ${e.message}", e)
//            }
//
//        }
//
//        log.info("$logName Sendte $antallVarslerSendt varsler")
//        tellMerVeiledningVarselSendt(antallVarslerSendt)
//
//        log.info("$logName Avslutter jobb")
//
//        return antallVarslerSendt
//    }
// }
