package com.beratyesbek.dslteamprbuddy

import com.beratyesbek.dslteamprbuddy.entity.EmailMessage
import com.beratyesbek.dslteamprbuddy.service.email.EmailSender
import com.beratyesbek.dslteamprbuddy.service.email.config.EmailConfig
import com.google.cloud.spring.secretmanager.SecretManagerTemplate
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import kotlin.test.assertEquals
import kotlin.test.assertTrue


@SpringBootTest(classes = [EmailConfig::class])
class MailjetEmailSenderSpringIT {

    companion object {
        private lateinit var server: MockWebServer

        @JvmStatic @BeforeAll
        fun start() { server = MockWebServer(); server.start() }
        @JvmStatic @AfterAll
        fun stop()  { server.shutdown() }

        @JvmStatic
        @DynamicPropertySource
        fun props(reg: DynamicPropertyRegistry) {
            reg.add("app.email.provider") { "mailjet" }
            reg.add("app.email.fromAddress") { "noreply@example.com" }
            reg.add("app.email.fromName") { "PR Buddy" }
            reg.add("app.email.mailjet.apiKeyPublicSecretName") { "pub" }
            reg.add("app.email.mailjet.apiKeyPrivateSecretName") { "priv" }
            reg.add("app.email.mailjet.baseUrl") { server.url("/").toString().removeSuffix("/") }
        }
    }

    @MockitoBean
    lateinit var secretManagerTemplate: SecretManagerTemplate

    @Autowired
    lateinit var emailSender: EmailSender

    @Test
    fun `posts v3_1 and parses success`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"Messages":[{"Status":"success","To":[{"Email":"me@example.com","MessageUUID":"abc","MessageID":456}]}]}"""
        ))

        whenever(secretManagerTemplate.getSecretString("pub")).thenReturn("PUBLIC_KEY")
        whenever(secretManagerTemplate.getSecretString("priv")).thenReturn("PRIVATE_KEY")

        val result = emailSender.send(
            EmailMessage(
                from = "PR Buddy <noreply@example.com>",
                to = listOf("me@example.com"),
                subject = "Test",
                htmlBody = "<b>Hello</b>",
                textBody = "Hello"
            )
        )
        assertTrue(result.ok)

        val req = server.takeRequest()
        assertEquals("/v3.1/send", req.path)
    }
}