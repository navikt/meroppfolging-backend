package no.nav.syfo

import java.util.UUID

const val NAV_CALL_ID_HEADER = "Nav-Call-Id"
const val NAV_CONSUMER_ID = "Nav-Consumer-Id"

fun createCallId(): String = UUID.randomUUID().toString()

fun createNavConsumerId() = "meroppfolging-backend"
