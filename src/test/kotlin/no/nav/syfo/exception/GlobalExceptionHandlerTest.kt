package no.nav.syfo.exception

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.syfo.senoppfolging.exception.AlreadyRespondedException
import no.nav.syfo.senoppfolging.exception.NoAccessToSenOppfolgingException
import no.nav.syfo.senoppfolging.exception.NoUtsendtVarselException
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import tools.jackson.module.kotlin.jacksonObjectMapper

class GlobalExceptionHandlerTest :
    FunSpec({
        val handler = GlobalExceptionHandler()
        val mapper = jacksonObjectMapper()

        listOf(
            AlreadyRespondedException() to "ALREADY_RESPONDED",
            NoUtsendtVarselException() to "NO_UTSENDT_VARSEL",
        ).forEach { (exception, code) ->
            test("conflict response identifies $code without changing the existing status or reason") {
                val response = handler.handleException(exception, MockHttpServletRequest())
                response.statusCode shouldBe HttpStatus.CONFLICT
                val json = mapper.readTree(mapper.writeValueAsString(response.body))
                json["reason"].asText() shouldBe "Conflict"
                json["error_code"].asText() shouldBe code
                json.size() shouldBe 2
            }
        }

        test("unrelated responses retain their existing representation") {
            val response = handler.handleException(NoAccessToSenOppfolgingException(), MockHttpServletRequest())
            response.statusCode shouldBe HttpStatus.FORBIDDEN
            val json = mapper.readTree(mapper.writeValueAsString(response.body))
            json.has("error_code") shouldBe false
        }
    })
