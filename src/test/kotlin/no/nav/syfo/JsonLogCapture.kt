package no.nav.syfo

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.OutputStreamAppender
import net.logstash.logback.encoder.LogstashEncoder
import org.slf4j.LoggerFactory
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.io.ByteArrayOutputStream

class JsonLogCapture(vararg classes: Class<*>) : AutoCloseable {
    private val output = ByteArrayOutputStream()
    private val context = LoggerFactory.getILoggerFactory() as LoggerContext
    private val encoder = LogstashEncoder().apply {
        context = this@JsonLogCapture.context
        start()
    }
    private val appender = OutputStreamAppender<ILoggingEvent>().apply {
        context = this@JsonLogCapture.context
        encoder = this@JsonLogCapture.encoder
        outputStream = output
        start()
    }
    private val loggers = classes.map { LoggerFactory.getLogger(it) as Logger }.onEach { it.addAppender(appender) }

    fun text(): String = output.toString(Charsets.UTF_8)

    fun events() = text().lineSequence().filter { it.isNotBlank() }.map { jacksonObjectMapper().readTree(it) }.toList()

    override fun close() {
        loggers.forEach { it.detachAppender(appender) }
        appender.stop()
        encoder.stop()
    }
}
