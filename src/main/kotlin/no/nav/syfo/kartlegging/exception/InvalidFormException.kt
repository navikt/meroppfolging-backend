package no.nav.syfo.kartlegging.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
class InvalidFormException (
    message: String = "Invalid form data",
    cause: Throwable? = null
) : RuntimeException(message, cause)
