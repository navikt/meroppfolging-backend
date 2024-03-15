package no.nav.syfo

import no.nav.security.token.support.spring.test.MockOAuth2ServerAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(MockOAuth2ServerAutoConfiguration::class)
class LocalApplicationSecurityConfig
