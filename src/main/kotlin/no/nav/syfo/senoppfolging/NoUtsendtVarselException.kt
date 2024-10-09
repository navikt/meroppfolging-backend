package no.nav.syfo.senoppfolging

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.CONFLICT, reason = "There is no utsendt varsel")
class NoUtsendtVarselException : RuntimeException()
