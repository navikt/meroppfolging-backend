package no.nav.syfo

import io.kotest.core.spec.style.DescribeSpec
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.kafka.test.context.EmbeddedKafka

@TestConfiguration
@EmbeddedKafka
@SpringBootTest(classes = [LocalApplication::class])
class ApplicationTests : DescribeSpec() {
    @Suppress("EmptyFunctionBlock")
    @Test
    fun contextLoads() {
    }
}
