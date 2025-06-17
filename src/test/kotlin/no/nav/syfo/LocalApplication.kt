package no.nav.syfo

import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.BEFORE_CLASS
import org.springframework.boot.fromApplication
import org.springframework.boot.test.context.TestConfiguration
@AutoConfigureEmbeddedDatabase(provider = ZONKY, refresh = BEFORE_CLASS)
@TestConfiguration(proxyBeanMethods = false)
class LocalApplication

fun main(args: Array<String>) {
    fromApplication<Application>().with(LocalApplication::class.java).run(*args)
}
