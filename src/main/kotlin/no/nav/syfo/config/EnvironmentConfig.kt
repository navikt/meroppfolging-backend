package no.nav.syfo.config

import org.springframework.core.env.Environment

fun Environment.isLocal(): Boolean {
    return this.activeProfiles.any {
        it.equals("local", ignoreCase = true) || it.equals("test", ignoreCase = true)
    }
}
