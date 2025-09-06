package com.beratyesbek.dslteamprbuddy

import com.beratyesbek.dslteamprbuddy.entity.EmailMessage
import com.beratyesbek.dslteamprbuddy.entity.User
import com.beratyesbek.dslteamprbuddy.service.AiMessageService
import com.beratyesbek.dslteamprbuddy.service.email.EmailSender
import com.beratyesbek.dslteamprbuddy.service.email.WeeklyAssignmentEmailComposer
import com.beratyesbek.dslteamprbuddy.service.email.WeeklyAssignmentEmailService
import com.beratyesbek.dslteamprbuddy.service.email.config.EmailConfig
import com.beratyesbek.dslteamprbuddy.service.email.impl.RecipientOverrideEmailSender
import com.google.cloud.spring.secretmanager.SecretManagerTemplate
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import kotlin.test.assertTrue

@SpringBootTest(classes = [EmailConfig::class, WeeklyAssignmentEmailService::class, WeeklyAssignmentEmailComposer::class])
@TestPropertySource(properties = [
    "app.email.provider=noop",
    "app.email.fromAddress=noreply@example.com",
    "app.email.fromName=PR Buddy",
    "app.email.recipientOverride=test+override@example.com",
    "app.jobs.assigner.enabled=false"
])
class WeeklyAssignmentEmailMultiReviewerTest {

    @MockitoBean
    lateinit var aiMessageService: AiMessageService

    @MockitoBean
    lateinit var secretManagerTemplate: SecretManagerTemplate

    @MockitoSpyBean
    lateinit var emailSender: EmailSender

    @Autowired
    lateinit var weeklyService: WeeklyAssignmentEmailService

    @Autowired
    lateinit var env: Environment

    @Test
    fun `sends one email per reviewer`() {
        whenever(aiMessageService.generateMessage(any(), any(), any(), any()))
            .thenReturn("stub ai message")

        val users = listOf(
            User(
                name = "Alice",
                email = "alice@acme.com",
                delta = 2,
                teamId = "DSL",
                isAvailable = true,
                mainLanguage = "English"
            ),
            User(
                name = "Bob",
                email = "bob@acme.com",
                delta = 2,
                teamId = "DSL",
                isAvailable = true,
                mainLanguage = "Turkish"
            ),
            User(
                name = "Cara",
                email = "cara@acme.com",
                delta = 2,
                teamId = "DSL",
                isAvailable = true,
                mainLanguage = "English"
            ),
            User(
                name = "DevA",
                email = "deva@acme.com",
                delta = 2,
                teamId = "DSL",
                isAvailable = true,
                mainLanguage = "English"
            )
        )

        // DevA reviewed by 3 reviewers - expect 3 emails
        val assignments = mapOf("DevA" to listOf("Alice", "Bob", "Cara"))

        weeklyService.sendWeeklyEmails(assignments, users, "DSL")

        val captor = argumentCaptor<EmailMessage>()
        verify(emailSender, times(3)).send(any())
        assertTrue(emailSender is RecipientOverrideEmailSender)
    }
}