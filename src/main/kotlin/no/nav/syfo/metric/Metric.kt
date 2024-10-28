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
    fun countSenOppfolgingRequestYes() = countEvent("sen_oppfolging_submitted_yes")
    fun countSenOppfolgingRequestNo() = countEvent("sen_oppfolging_submitted_yes")
    fun countSenOppfolgingSubmittedYes() = countEvent("sen_oppfolging_submitted_yes")
    fun countSenOppfolgingSubmittedNO() = countEvent("sen_oppfolging_submitted_no")

    fun countCallVeilarbregistreringComplete() = countEvent("call_veilarbregistrering_complete")

    fun countSenOppfolgingV2Submitted() = countEvent("sen_oppfolging_V2_submitted")

    fun countEvent(name: String) {
        registry
            .counter(
                metricPrefix(name),
                Tags.of("type", "info"),
            ).increment()
    }

    private fun metricPrefix(name: String) = "meroppfolging_backend_$name"
}
