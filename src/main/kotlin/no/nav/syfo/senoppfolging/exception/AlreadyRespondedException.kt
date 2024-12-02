package no.nav.syfo.senoppfolging.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.CONFLICT, reason = "User has already responded in the last 3 months")
class AlreadyRespondedException : RuntimeException()
