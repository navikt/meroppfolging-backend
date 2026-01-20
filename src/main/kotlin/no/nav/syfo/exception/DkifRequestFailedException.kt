package no.nav.syfo.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
class DkifRequestFailedException(message: String = "") :
    RuntimeException("Request to get Kontaktinformasjon from KRR Failed: $message")
