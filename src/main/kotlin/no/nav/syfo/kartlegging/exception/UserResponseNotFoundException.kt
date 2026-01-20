package no.nav.syfo.kartlegging.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.NOT_FOUND)
class UserResponseNotFoundException(message: String = "User response not found", cause: Throwable? = null) :
    RuntimeException(message, cause)
