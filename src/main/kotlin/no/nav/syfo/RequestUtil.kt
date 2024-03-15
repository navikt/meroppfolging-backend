package no.nav.syfo

import java.util.UUID

const val NAV_CALL_ID_HEADER = "Nav-Call-Id"

fun createCallId(): String = UUID.randomUUID().toString()
