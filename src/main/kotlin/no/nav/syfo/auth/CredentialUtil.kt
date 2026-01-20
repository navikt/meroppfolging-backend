package no.nav.syfo.auth

fun bearerHeader(token: String?): String = "Bearer $token"
