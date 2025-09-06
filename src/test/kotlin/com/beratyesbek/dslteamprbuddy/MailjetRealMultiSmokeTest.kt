package com.beratyesbek.dslteamprbuddy

import com.beratyesbek.dslteamprbuddy.entity.User
import com.beratyesbek.dslteamprbuddy.service.email.WeeklyAssignmentEmailService
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * Sends real emails (all to same inbox via recipientOverride).
 * Enable manually and make sure ADC or SA creds are set.
 */
@SpringBootTest
@ActiveProfiles("smoketest")
@Disabled("Enable manually to send a real emails")
class MailjetRealMultiSmokeTest(
    @Autowired private val weeklyService: WeeklyAssignmentEmailService
) {

    @Test
    fun `send multiple emails to myself via override`() {
        val reviewers = (1..5).map { i ->
            User(
                name = "Reviewer$i",
                email = "reviewer$i@example.com",
                delta = 2,
                teamId = "DSL",
                isAvailable = true,
                mainLanguage = "English"
            )
        }

        val dev = User(
            name = "DevA",
            email = "devA@example.com",
            delta = 2,
            teamId = "DSL",
            isAvailable = true,
            mainLanguage = "English"
        )

        val users = reviewers + dev

        val assignments = mapOf("DevA" to reviewers.map { it.name })

        weeklyService.sendWeeklyEmails(assignments, users, "DSL")
    }
}