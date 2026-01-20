package no.nav.syfo.auth.azuread

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate

class AzureAdClientTest :
    DescribeSpec(
        {
            val restTemplate = mockk<RestTemplate>()
            val azureAdClient = AzureAdClient(
                azureAppClientId = "test-client-id",
                azureAppClientSecret = "test-client-secret",
                azureTokenEndpoint = "https://test.token.endpoint",
                restTemplate = restTemplate,
                env = mockk(relaxed = true),
            )

            beforeTest {
                clearAllMocks()
                azureAdClient.clearTokenCache()
            }

            it("Gets cached token if not expired") {
                val testTokenResponse = AzureAdTokenResponse(
                    access_token = "some-access-token",
                    expires_in = 3600,
                )
                every {
                    restTemplate.exchange(
                        any<String>(),
                        any(),
                        any(),
                        AzureAdTokenResponse::class.java,
                    )
                } returns ResponseEntity(testTokenResponse, HttpStatus.OK)

                val adToken = azureAdClient.getSystemToken("some-random-scope")
                val cachedAdToken = azureAdClient.getSystemToken("some-random-scope")

                adToken shouldBe "some-access-token"
                cachedAdToken shouldBe "some-access-token"
                verify(exactly = 1) {
                    restTemplate.exchange(any<String>(), any(), any(), AzureAdTokenResponse::class.java)
                }
            }

            it("getSystemToken throws AzureAdClientException if request fails") {
                every {
                    restTemplate.exchange(
                        any<String>(),
                        any(),
                        any(),
                        any<Class<AzureAdTokenResponse>>(),
                    )
                } throws RestClientResponseException(
                    "error",
                    500,
                    "Internal Server Error",
                    HttpHeaders.EMPTY,
                    ByteArray(0),
                    Charsets.UTF_8,
                )

                val exception = shouldThrow<AzureAdClientException> {
                    azureAdClient.getSystemToken("some-random-scope")
                }
                exception.message shouldBe "Failed to get AzureADToken for scope: some-random-scope"
            }
        },
    )
