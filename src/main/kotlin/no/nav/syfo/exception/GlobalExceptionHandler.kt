package no.nav.syfo.exception

import jakarta.servlet.http.HttpServletRequest
import no.nav.security.token.support.core.exceptions.JwtTokenInvalidClaimException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import no.nav.syfo.kartlegging.exception.InvalidFormException
import no.nav.syfo.kartlegging.exception.KandidatNotFoundException
import no.nav.syfo.kartlegging.exception.UserResponseNotFoundException
import no.nav.syfo.logger
import no.nav.syfo.senoppfolging.exception.AlreadyRespondedException
import no.nav.syfo.senoppfolging.exception.NoAccessToSenOppfolgingException
import no.nav.syfo.senoppfolging.exception.NoUtsendtVarselException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {
    private val log = logger()

    @ExceptionHandler(java.lang.Exception::class)
    fun handleException(ex: Exception, request: HttpServletRequest,): ResponseEntity<Any> = when (ex) {
        is AbstractApiError -> {
            when (ex.loglevel) {
                LogLevel.WARN -> log.warn(ex.message, ex)
                LogLevel.ERROR -> log.error(ex.message, ex)
                LogLevel.OFF -> {}
            }

            ResponseEntity(ApiError(ex.reason), ex.httpStatus)
        }

        is JwtTokenInvalidClaimException -> createResponseEntity(HttpStatus.UNAUTHORIZED)
        is JwtTokenUnauthorizedException -> createResponseEntity(HttpStatus.UNAUTHORIZED)
        is HttpMediaTypeNotAcceptableException -> createResponseEntity(HttpStatus.NOT_ACCEPTABLE)
        is AlreadyRespondedException -> createResponseEntity(HttpStatus.CONFLICT, ex)
        is NoAccessToSenOppfolgingException -> createResponseEntity(HttpStatus.FORBIDDEN, ex)
        is NoUtsendtVarselException -> createResponseEntity(HttpStatus.CONFLICT, ex)
        is InvalidFormException -> createResponseEntity(HttpStatus.BAD_REQUEST, ex)
        is UserResponseNotFoundException -> createResponseEntity(HttpStatus.NOT_FOUND, ex)
        is KandidatNotFoundException -> createResponseEntity(HttpStatus.NOT_FOUND, ex)
        else -> {
            log.error("Internal server error - ${ex.message} - ${request.method}: ${request.requestURI}", ex)
            createResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}

private fun createResponseEntity(status: HttpStatus): ResponseEntity<Any> =
    ResponseEntity(ApiError(status.reasonPhrase), status)

private fun createResponseEntity(status: HttpStatus, ex: RuntimeException): ResponseEntity<Any> =
    ResponseEntity(ApiError(reason = ex.message ?: HttpStatus.CONFLICT.reasonPhrase), status)

private data class ApiError(val reason: String)

abstract class AbstractApiError(
    message: String,
    val httpStatus: HttpStatus,
    val reason: String,
    val loglevel: LogLevel,
    grunn: Throwable? = null,
) : RuntimeException(message, grunn)

enum class LogLevel {
    WARN,
    ERROR,
    OFF,
}
