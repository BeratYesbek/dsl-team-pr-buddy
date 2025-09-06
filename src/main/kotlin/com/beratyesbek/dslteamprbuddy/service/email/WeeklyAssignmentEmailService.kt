package com.beratyesbek.dslteamprbuddy.service.email

import EmailProperties
import com.beratyesbek.dslteamprbuddy.entity.User
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class WeeklyAssignmentEmailService(
    private val emailSender: EmailSender,
    private val composer: WeeklyAssignmentEmailComposer,
    private val props: EmailProperties
) {
    private val log = LoggerFactory.getLogger(WeeklyAssignmentEmailService::class.java)

    /**
     * Sends one email per reviewer with their dev list + the full table.
     * @param assignments map of Dev -> List(Reviewers)
     * @param users all team users (used for emails/language lookup)
     * @param teamName displayed in the AI-generated intro
     */
    fun sendWeeklyEmails(
        assignments: Map<String, List<String>>,
        users: List<User>,
        teamName: String
    ) {
        val usersByName = users.associateBy { it.name }

        // reviewer -> list of devs they review
        val reviewerToDevs = mutableMapOf<String, MutableList<String>>()
        for ((dev, reviewers) in assignments) {
            reviewers.forEach { reviewer ->
                reviewerToDevs.computeIfAbsent(reviewer) { mutableListOf() }.add(dev)
            }
        }

        reviewerToDevs.forEach { (reviewerName, devList) ->
            val reviewer = usersByName[reviewerName]
            if (reviewer == null) {
                log.warn("No user record for reviewer '{}'; skipping", reviewerName)
                return@forEach
            }
            if (reviewer.email.isBlank()) {
                log.warn("No email for reviewer '{}'; skipping", reviewerName)
                return@forEach
            }

            val message = composer.composeForReviewer(
                reviewer = reviewer,
                devList = devList.sorted(),
                teamName = teamName,
                assignments = assignments,
                fromAddress = props.fromAddress,
                fromName = props.fromName
            )

            val result = emailSender.send(message)
            if (result.ok) {
                log.info("Sent weekly assignment email via {} to {} ({} devs)",
                    result.provider, reviewer.email, devList.size)
            } else {
                log.error("Failed to send email via {} to {}: {}",
                    result.provider, reviewer.email, result.error)
            }
        }
    }
}