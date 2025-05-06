package no.nav.syfo

import org.springframework.boot.fromApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class LocalApplication {

    @Bean
    @ServiceConnection
    fun kafkaContainer(): KafkaContainer {
        return KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"))
    }

    @Bean
    @ServiceConnection
    fun dbContainer(): PostgreSQLContainer<Nothing> {
        return PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:17-alpine")).apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }
    }
}

fun main(args: Array<String>) {
    fromApplication<Application>().with(LocalApplication::class.java).run(*args)
}
