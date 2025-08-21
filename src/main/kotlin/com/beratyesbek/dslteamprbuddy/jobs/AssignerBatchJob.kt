package com.beratyesbek.dslteamprbuddy.jobs

import com.beratyesbek.dslteamprbuddy.repository.DefaultReviewerRepository
import com.beratyesbek.dslteamprbuddy.repository.TeamRepository
import com.beratyesbek.dslteamprbuddy.repository.UserRepository
import com.beratyesbek.dslteamprbuddy.service.AiMessageService
import jakarta.mail.internet.MimeMessage
import lombok.RequiredArgsConstructor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import org.springframework.web.util.HtmlUtils
import java.time.LocalDateTime
import kotlin.random.Random

@Component
@RequiredArgsConstructor
class AssignerBatchJob(
    val aiMessageService: AiMessageService,
    val userRepository: UserRepository,
    val teamRepository: TeamRepository,
    val defaultReviewerRepository: DefaultReviewerRepository,
    val mailSender: JavaMailSender
) {

    @Value("\${spring.mail.username}")
    var from: String = ""

    @Value("\${team.teamId}")
    var teamId: String = ""


    @Bean
    fun taskRunner() = ApplicationRunner { _: ApplicationArguments ->
        val logger = LoggerFactory.getLogger(AssignerBatchJob::class.java)
        val team = teamRepository.findById(teamId).block()
        val users = userRepository.findAllByTeamId(team?.id ?: "unknown")
            .collectList()
            .block() ?: emptyList()
        val defaultReviewers = defaultReviewerRepository.findAll().collectList().block() ?: emptyList()

        val devs = users.filter { it.isAvailable }.map { it.name }
        val maxReviews = users.associate { it.name to it.delta }
        val emails = users.associate { it.name to it.email }

        val defaultMap =
            defaultReviewers.groupBy { it.devName }.mapValues { entry -> entry.value.map { it.reviewerName } }

        val shuffledDevs = devs.shuffled()

        val assignments = mutableMapOf<String, MutableList<String>>()
        devs.forEach { assignments[it] = mutableListOf() }

        val loads = mutableMapOf<String, Int>()
        devs.forEach { loads[it] = 0 }

        for (dev in devs) {
            val defaults = defaultMap[dev] ?: emptyList()
            for (reviewer in defaults) {
                if (reviewer !in devs) {
                    logger.warn("Default reviewer $reviewer not in devs list; skipping")
                    continue
                }
                assignments[dev]?.add(reviewer)
                loads[reviewer] = (loads[reviewer] ?: 0) + 1
            }
        }

        for (dev in shuffledDevs) {
            if (assignments[dev]?.size ?: 0 >= 1) continue
            val possibles = devs.filter {
                it != dev && it !in (assignments[dev] ?: emptyList()) && (loads[it] ?: 0) < maxReviews.getOrDefault(
                    it,
                    0
                ) && maxReviews.getOrDefault(it, 0) > 0
            }
            if (possibles.isEmpty()) {
                logger.warn("No available reviewers for $dev; skipping")
                continue
            }
            val minLoad = possibles.minOf { loads[it] ?: 0 }
            val minPoss = possibles.filter { (loads[it] ?: 0) == minLoad }
            val reviewer = minPoss.random()
            assignments[dev]?.add(reviewer)
            loads[reviewer] = (loads[reviewer] ?: 0) + 1
        }

        for (dev in devs) {
            val numDefaults = defaultMap[dev]?.size ?: 0
            if (numDefaults == 1 && (assignments[dev]?.size ?: 0) == 1) {
                val possibles = devs.filter {
                    it != dev && it !in (assignments[dev] ?: emptyList()) && (loads[it] ?: 0) < maxReviews.getOrDefault(
                        it,
                        0
                    ) && maxReviews.getOrDefault(it, 0) > 0
                }
                if (possibles.isEmpty()) {
                    logger.warn("Cannot add second reviewer for dev $dev with one default; no available options")
                    continue
                }
                val minLoad = possibles.minOf { loads[it] ?: 0 }
                val minPoss = possibles.filter { (loads[it] ?: 0) == minLoad }
                val reviewer = minPoss.random()
                assignments[dev]?.add(reviewer)
                loads[reviewer] = (loads[reviewer] ?: 0) + 1
            }
        }

        for (dev in devs) {
            while ((assignments[dev]?.size ?: 0) < 2) {
                val possibles = devs.filter {
                    it != dev && it !in (assignments[dev] ?: emptyList()) && (loads[it] ?: 0) < maxReviews.getOrDefault(
                        it,
                        0
                    ) && maxReviews.getOrDefault(it, 0) > 0
                }
                if (possibles.isEmpty()) {
                    logger.warn("Cannot add more reviewers to $dev to reach 2; no available options")
                    break
                }
                val minLoad = possibles.minOf { loads[it] ?: 0 }
                val minPoss = possibles.filter { (loads[it] ?: 0) == minLoad }
                val reviewer = minPoss.random()
                assignments[dev]?.add(reviewer)
                loads[reviewer] = (loads[reviewer] ?: 0) + 1
            }
        }

        val numToAddSecond = Random.nextInt(0, devs.size + 1)
        val candidates = devs.shuffled().toMutableList()
        for (i in 0 until numToAddSecond) {
            if (candidates.isEmpty()) break
            val dev = candidates.removeFirst()
            if ((assignments[dev]?.size ?: 0) >= 2) continue
            val possibles = devs.filter {
                it != dev && it !in (assignments[dev] ?: emptyList()) && (loads[it] ?: 0) < maxReviews.getOrDefault(
                    it,
                    0
                ) && maxReviews.getOrDefault(it, 0) > 0
            }
            if (possibles.isEmpty()) continue
            val minLoad = possibles.minOf { loads[it] ?: 0 }
            val minPoss = possibles.filter { (loads[it] ?: 0) == minLoad }
            val reviewer = minPoss.random()
            assignments[dev]?.add(reviewer)
            loads[reviewer] = (loads[reviewer] ?: 0) + 1
        }

        val reviewerToDevs = mutableMapOf<String, MutableList<String>>()
        for ((dev, reviewers) in assignments) {
            for (reviewer in reviewers) {
                reviewerToDevs.getOrPut(reviewer) { mutableListOf() }.add(dev)
            }
        }

        val tableHtml = buildString {
            append("<table border=\"1\" style=\"border-collapse: collapse; width: 50%;\">")
            append("<tr><th>Dev</th><th>Reviewers</th></tr>")
            for ((dev, reviewers) in assignments.toSortedMap()) {
                append("<tr><td>$dev</td><td>${reviewers.joinToString(", ")}</td></tr>")
            }
            append("</table>")
        }

        for ((reviewer, devList) in reviewerToDevs) {
            val emailAddress = emails[reviewer]
            if (emailAddress != null) {
                val message: MimeMessage = mailSender.createMimeMessage()
                val helper = MimeMessageHelper(message, true)
                helper.setFrom(from)
                helper.setTo(emailAddress)
                helper.setSubject("Your PR Review Assignments for This Week")
                val generatedAiMessage = aiMessageService.generateMessage(
                    reviewer,
                    devList,
                    team?.name ?: "Unknown Team",
                    users.find { it.name == reviewer }?.mainLanguage ?: "English"
                )
                val aiMessageHtml = HtmlUtils.htmlEscape(generatedAiMessage?.trim().orEmpty())
                    .replace("\r\n", "\n")
                    .replace("\r", "\n")
                    .replace("\n", "<br>")

                val emailBody = """
                    <html>
                    <body>
                    $aiMessageHtml
                    <p>Full assignments for the team:</p>
                    $tableHtml
                    </body>
                    </html>
                """.trimIndent()

                helper.setText(emailBody, true)
                mailSender.send(message)
                logger.info("Email sent to $reviewer ($emailAddress) for reviewing ${devList.size} devs")
            } else {
                logger.warn("No email found for reviewer: $reviewer")
            }
        }

        logger.info("All emails processed at: ${LocalDateTime.now()}")
        System.exit(0)
    }

}