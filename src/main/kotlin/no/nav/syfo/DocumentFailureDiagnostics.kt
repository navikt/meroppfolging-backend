package no.nav.syfo

import org.springframework.web.client.RestClientResponseException
import java.net.SocketException

// Document failures may contain form answers, response bodies and identifiers in exception messages.
// Keep the original exception classes and stack locations, without copying those messages.
internal fun Throwable.documentFailureFields(): Map<String, Any> = buildMap {
    put("exception_type", this@documentFailureFields.javaClass.name)
    cause?.let { put("cause_type", it.javaClass.name) }
    if (this@documentFailureFields is RestClientResponseException) {
        statusCode.value().takeIf { it in 100..599 }?.let { put("upstream_status", it) }
    }
    put(
        "stack_trace",
        generateSequence(this@documentFailureFields) { it.cause }.take(8).joinToString("\nCaused by: ") {
            // This fixed JDK transport message explains the observed EOF without logging arbitrary socket text.
            val message = if (it is SocketException && it.message == "Unexpected end of file from server") {
                ": Unexpected end of file from server"
            } else {
                ""
            }
            it.javaClass.name + message + it.stackTrace.joinToString("", prefix = "\n") { frame -> "\tat $frame\n" }
        },
    )
}
