package no.nav.syfo

import io.kotest.core.spec.style.AnnotationSpec.AfterAll
import io.kotest.core.spec.style.AnnotationSpec.BeforeAll
import io.kotest.core.spec.style.DescribeSpec
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

abstract class IntegrationTest : DescribeSpec() {
    companion object {
        private val db =
            PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:15-alpine")).apply {
                withDatabaseName("testdb")
                withUsername("test")
                withPassword("test")
            }

        @BeforeAll
        @JvmStatic
        fun startDBContainer() {
            db.start()
        }

        @AfterAll
        @JvmStatic
        fun stopDBContainer() {
            db.stop()
        }

        @DynamicPropertySource
        @JvmStatic
        fun registerDBContainer(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", db::getJdbcUrl)
            registry.add("spring.datasource.username", db::getUsername)
            registry.add("spring.datasource.password", db::getPassword)
        }

        init {
            db.start()
        }
    }
}
