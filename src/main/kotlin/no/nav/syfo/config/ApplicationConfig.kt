package no.nav.syfo.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.web.client.RestTemplate

@Configuration
@EnableTransactionManagement
@EnableScheduling
class ApplicationConfig {
    @Primary
    @Bean
    fun restTemplate() = RestTemplate()
}
