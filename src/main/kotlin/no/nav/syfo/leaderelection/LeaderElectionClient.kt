package no.nav.syfo.leaderelection

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.syfo.config.kafka.jacksonMapper
import no.nav.syfo.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.net.InetAddress

@Component
class LeaderElectionClient(
    @Value("\${ELECTOR_GET_URL}") private val electorPath: String,
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper = jacksonMapper(),
) {
    val log = logger()

    fun isPodLeader(): Boolean {
        val leaderPod = getLeaderPod(electorPath)
        val podHostname = InetAddress.getLocalHost().hostName
        return podHostname == leaderPod
    }

    private fun getLeaderPod(path: String): String {
        val leaderJsonString = callElectorPath(path)
        return parseLeaderJson(leaderJsonString)
    }

    private fun callElectorPath(path: String): String? {
        return try {
            restTemplate.getForObject(path, String::class.java)
        } catch (e: HttpClientErrorException) {
            log.error("Error calling elector path: ${e.message}", e)
            null
        }
    }

    private fun parseLeaderJson(leaderJsonString: String?): String {
        return leaderJsonString?.let {
            val leaderJson = objectMapper.readTree(it)
            leaderJson["name"].asText()
        } ?: error("Failed to retrieve leader pod information")
    }
}
