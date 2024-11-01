package no.nav.syfo.metric

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class Metric
@Autowired
constructor(
    private val registry: MeterRegistry,
) {
    fun countSenOppfolgingSubmitted() = countEvent("sen_oppfolging_submitted")
    fun countSenOppfolgingRequestYes() = countEvent("sen_oppfolging_requested_yes")
    fun countSenOppfolgingRequestNo() = countEvent("sen_oppfolging_requested_no")
    fun countSenOppfolgingFerdigstilt() = countEvent("sen_oppfolging_ferdigstilt")

    fun countCallVeilarbregistreringComplete() = countEvent("call_veilarbregistrering_complete")

    fun countSenOppfolgingV2Submitted() = countEvent("sen_oppfolging_V2_submitted")
    fun countSenOppfolgingV2SubmittedNo() = countEvent("sen_oppfolging_V2_submitted_no")
    fun countSenOppfolgingVarslerToBeSent(count: Double) = countEvent("sen_oppfolging_varsler_to_be_sent", count)

    fun countEvent(name: String, count: Double = 1.0) {
        registry
            .counter(
                metricPrefix(name),
                Tags.of("type", "info"),
            ).increment(count)
    }

    private fun metricPrefix(name: String) = "meroppfolging_backend_$name"
}
