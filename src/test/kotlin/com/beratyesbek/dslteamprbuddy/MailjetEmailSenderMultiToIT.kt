package com.beratyesbek.dslteamprbuddy

import com.beratyesbek.dslteamprbuddy.entity.EmailMessage
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
class MailjetEmailSenderMultiToIT {

    companion object {
        private lateinit var server: MockWebServer

        @BeforeAll @JvmStatic
        fun setup() { server = MockWebServer(); server.start() }

        @AfterAll @JvmStatic
        fun teardown() { server.shutdown() }

        @DynamicPropertySource @JvmStatic
        fun props(reg: DynamicPropertyRegistry) {
            reg.add("app.email.provider") { "mailjet" }
            reg.add("app.email.fromAddress") { "noreply@example.com" }
            reg.add("app.email.fromName") { "PR Buddy" }
            reg.add("app.email.mailjet.apiKeyPublicSecretName") { "pub" }
            reg.add("app.email.mailjet.apiKeyPrivateSecretName") { "priv" }
            reg.add("app.email.mailjet.baseUrl") { server.url("/").toString().removeSuffix("/") }
            reg.add("app.jobs.assigner.enabled") { "false" }
        }
    }

    @MockitoBean lateinit var secretManagerTemplate: SecretManagerTemplate

    @Autowired lateinit var emailSender: EmailSender

    @Test
    fun `encodes multiple To recipients in one message`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"Messages":[{"Status":"success","To":[{"Email":"a@x"},{"Email":"b@y"}]}]}"""
        ))
        whenever(secretManagerTemplate.getSecretString("pub")).thenReturn("PUBLIC")
        whenever(secretManagerTemplate.getSecretString("priv")).thenReturn("PRIVATE")

        val result = emailSender.send(
            EmailMessage(
                from = "PR Buddy <noreply@example.com>",
                to = listOf("a@x","b@y"),
                subject = "Multi-To Test",
                htmlBody = "<p>hi</p>",
                textBody = "hi"
            )
        )
        assert(result.ok)

        val req = server.takeRequest()
        assert(req.path == "/v3.1/send")
        val json = JSONObject(req.body.readUtf8())
        val toArr = json.getJSONArray("Messages")
            .getJSONObject(0)
            .getJSONArray("To")
        assert(toArr.length() == 2)
    }
}