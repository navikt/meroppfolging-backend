package no.nav.syfo.config

import org.springframework.core.env.Environment

fun Environment.isLocal(): Boolean = this.activeProfiles.any {
    it.equals("local", ignoreCase = true) || it.equals("test", ignoreCase = true)
}
