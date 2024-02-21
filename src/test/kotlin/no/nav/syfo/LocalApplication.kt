package no.nav.syfo

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableJwtTokenValidation
@EnableMockOAuth2Server
class LocalApplication

fun main(args: Array<String>) {
    runApplication<LocalApplication>(*args)
}
