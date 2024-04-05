package no.nav.syfo

import no.nav.security.token.support.spring.test.MockOAuth2ServerAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

@Configuration
@Import(MockOAuth2ServerAutoConfiguration::class)
@Profile("test")
class LocalApplicationConfig {
    @Bean
    fun jdbcTemplate(dataSource: DataSource) = JdbcTemplate(dataSource)
}
