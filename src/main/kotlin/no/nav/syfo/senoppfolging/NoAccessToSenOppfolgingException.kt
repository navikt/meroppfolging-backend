package no.nav.syfo.senoppfolging

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "No access")
class NoAccessToSenOppfolgingException : RuntimeException()
