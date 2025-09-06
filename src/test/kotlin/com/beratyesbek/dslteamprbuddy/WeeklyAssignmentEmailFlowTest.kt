package com.beratyesbek.dslteamprbuddy

import com.beratyesbek.dslteamprbuddy.service.email.EmailSender
import com.beratyesbek.dslteamprbuddy.service.email.config.EmailConfig
import com.google.cloud.spring.secretmanager.SecretManagerTemplate
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.jupiter.api.*
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest(classes = [EmailConfig::class])
class WeeklyAssignmentEmailFlowTest {

    companion object {
        private lateinit var server: MockWebServer

        @JvmStatic @BeforeAll
        fun start() {
            server = MockWebServer()
            server.start()
        }

        @JvmStatic @AfterAll
        fun stop() {
            server.shutdown()
        }

        @JvmStatic
        @DynamicPropertySource
        fun props(reg: DynamicPropertyRegistry) {
            reg.add("app.email.provider") { "mailjet" }
            reg.add("app.email.fromAddress") { "noreply@example.com" }
            reg.add("app.email.fromName") { "PR Buddy" }
            reg.add("app.email.mailjet.apiKeyPublicSecretName") { "pub" }
            reg.add("app.email.mailjet.apiKeyPrivateSecretName") { "priv" }
            reg.add("app.email.mailjet.baseUrl") {
                server.url("/").toString().removeSuffix("/")
            }
        }
    }

    @MockitoBean
    lateinit var secretManagerTemplate: SecretManagerTemplate

    @Autowired
    lateinit var emailSender: EmailSender

    @Test
    fun `posts v3_1 payload and parses success`() {
        // fake Mailjet success
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"Messages":[{"Status":"success","To":[{"Email":"me@example.com","MessageUUID":"abc-123","MessageID":456}]}]}
                """.trimIndent()
            )
        )

        // secrets returned by Secret Manager
        whenever(secretManagerTemplate.getSecretString("pub")).thenReturn("PUBLIC_KEY")
        whenever(secretManagerTemplate.getSecretString("priv")).thenReturn("PRIVATE_KEY")

        val result = emailSender.send(
            com.beratyesbek.dslteamprbuddy.entity.EmailMessage(
                from = "PR Buddy <noreply@example.com>",
                to = listOf("me@example.com"),
                subject = "Test",
                htmlBody = "<b>Hello</b>",
                textBody = "Hello"
            )
        )

        Assertions.assertTrue(result.ok)

        val req = server.takeRequest()
        Assertions.assertEquals("/v3.1/send", req.path)
        val bodyJson = JSONObject(req.body.readUtf8())
        val first = bodyJson.getJSONArray("Messages").getJSONObject(0)
        Assertions.assertEquals("Test", first.getString("Subject"))
        Assertions.assertEquals("me@example.com", first.getJSONArray("To").getJSONObject(0).getString("Email"))
    }
}