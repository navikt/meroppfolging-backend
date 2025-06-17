package no.nav.syfo

import io.kotest.core.spec.style.DescribeSpec
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.BEFORE_CLASS
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.kafka.test.context.EmbeddedKafka

@TestConfiguration
@EmbeddedKafka
@AutoConfigureEmbeddedDatabase(provider = ZONKY, refresh = BEFORE_CLASS)
@SpringBootTest(classes = [LocalApplication::class])
class ApplicationTests : DescribeSpec() {
    @Suppress("EmptyFunctionBlock")
    @Test
    fun contextLoads() {
    }
}
