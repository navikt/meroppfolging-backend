package no.nav.syfo.kartlegging.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.FORBIDDEN)
class NotKandidatException(
    message: String = "Personen er ikke kandidat for kartlegging",
    cause: Throwable? = null
) : RuntimeException(message, cause)
