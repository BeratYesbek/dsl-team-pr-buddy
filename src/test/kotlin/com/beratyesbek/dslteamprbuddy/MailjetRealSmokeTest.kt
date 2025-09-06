package com.beratyesbek.dslteamprbuddy

import com.beratyesbek.dslteamprbuddy.entity.EmailMessage
import com.beratyesbek.dslteamprbuddy.service.email.EmailSender
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertTrue

/**
 * This test sends a REAL email using Mailjet and your secrets in GCP.
 * Enable manually when you want to smoke test.
 */
@SpringBootTest
@ActiveProfiles("smoketest") // loads application-smoketest.properties
@Disabled("Enable manually to send a real email")
class MailjetRealSmokeTest(
    @Autowired private val emailSender: EmailSender
) {

    @Test
    fun `send real email to myself`() {
        val result = emailSender.send(
            EmailMessage(
                from = "PR Buddy <prbuddy@test.com>",
                to = listOf("hello@example.com"), // will be replaced by application-smoketest.properties.app.email.recipientOverride
                subject = "Smoke test from PR Buddy",
                htmlBody = "<p>Hello from Mailjet via PR Buddy!</p>",
                textBody = "Hello from Mailjet via PR Buddy!"
            )
        )
        assertTrue(result.ok, "Expected email send to succeed, got error=${result.error}")
    }
}