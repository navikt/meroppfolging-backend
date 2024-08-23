package no.nav.syfo

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.testcontainers.perSpec
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

abstract class IntegrationTest : DescribeSpec() {
    companion object {
        private val postgresContainer = PostgreSQLContainer<Nothing>("postgres:13.3").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerPgProperties(registry: DynamicPropertyRegistry) {
            postgresContainer.start()
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
        }
    }

    init {
        listener(postgresContainer.perSpec())

        afterSpec {
            postgresContainer.stop()
        }
    }
}
