package no.nav.syfo

import java.util.UUID

const val NAV_CALL_ID_HEADER = "Nav-Call-Id"
const val NAV_PERSONIDENT_HEADER = "nav-personident"
const val NAV_CONSUMER_ID_HEADER = "Nav-Consumer-Id"
const val MEROPPFOLGING_BACKEND_CONSUMER_ID = "meroppfolging-backend"

fun createCallId(): String = UUID.randomUUID().toString()
