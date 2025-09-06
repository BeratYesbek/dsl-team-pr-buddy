package com.beratyesbek.dslteamprbuddy.jobs

import com.beratyesbek.dslteamprbuddy.repository.DefaultReviewerRepository
import com.beratyesbek.dslteamprbuddy.repository.TeamRepository
import com.beratyesbek.dslteamprbuddy.repository.UserRepository
import com.beratyesbek.dslteamprbuddy.service.email.WeeklyAssignmentEmailService
import lombok.RequiredArgsConstructor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import kotlin.random.Random

@ConditionalOnProperty(prefix = "app.jobs.assigner", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@Component
@RequiredArgsConstructor
class AssignerBatchJob(
    private val userRepository: UserRepository,
    private val teamRepository: TeamRepository,
    private val defaultReviewerRepository: DefaultReviewerRepository,
    private val weeklyEmailService: WeeklyAssignmentEmailService
) {

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

        val defaultMap = defaultReviewers
            .groupBy { it.devName }
            .mapValues { entry -> entry.value.map { it.reviewerName } }

        val shuffledDevs = devs.shuffled()

        val assignments = mutableMapOf<String, MutableList<String>>()
        devs.forEach { assignments[it] = mutableListOf() }

        val loads = mutableMapOf<String, Int>()
        devs.forEach { loads[it] = 0 }

        // seed with defaults
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

        // make sure at least one reviewer
        for (dev in shuffledDevs) {
            if ((assignments[dev]?.size ?: 0) >= 1) continue
            val possibles = devs.filter {
                it != dev &&
                        it !in (assignments[dev] ?: emptyList()) &&
                        (loads[it] ?: 0) < maxReviews.getOrDefault(it, 0) &&
                        maxReviews.getOrDefault(it, 0) > 0
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

        // special case: only one default -> add one more
        for (dev in devs) {
            val numDefaults = defaultMap[dev]?.size ?: 0
            if (numDefaults == 1 && (assignments[dev]?.size ?: 0) == 1) {
                val possibles = devs.filter {
                    it != dev &&
                            it !in (assignments[dev] ?: emptyList()) &&
                            (loads[it] ?: 0) < maxReviews.getOrDefault(it, 0) &&
                            maxReviews.getOrDefault(it, 0) > 0
                }
                if (possibles.isEmpty()) {
                    logger.warn("Cannot add second reviewer for dev $dev; no available options")
                    continue
                }
                val minLoad = possibles.minOf { loads[it] ?: 0 }
                val minPoss = possibles.filter { (loads[it] ?: 0) == minLoad }
                val reviewer = minPoss.random()
                assignments[dev]?.add(reviewer)
                loads[reviewer] = (loads[reviewer] ?: 0) + 1
            }
        }

        // fill up to two reviewers
        for (dev in devs) {
            while ((assignments[dev]?.size ?: 0) < 2) {
                val possibles = devs.filter {
                    it != dev &&
                            it !in (assignments[dev] ?: emptyList()) &&
                            (loads[it] ?: 0) < maxReviews.getOrDefault(it, 0) &&
                            maxReviews.getOrDefault(it, 0) > 0
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

        // randomly add some second reviewers (if capacity)
        val numToAddSecond = Random.nextInt(0, devs.size + 1)
        val candidates = devs.shuffled().toMutableList()
        repeat(numToAddSecond) {
            if (candidates.isEmpty()) return@repeat
            val dev = candidates.removeFirst()
            if ((assignments[dev]?.size ?: 0) >= 2) return@repeat
            val possibles = devs.filter {
                it != dev &&
                        it !in (assignments[dev] ?: emptyList()) &&
                        (loads[it] ?: 0) < maxReviews.getOrDefault(it, 0) &&
                        maxReviews.getOrDefault(it, 0) > 0
            }
            if (possibles.isEmpty()) return@repeat
            val minLoad = possibles.minOf { loads[it] ?: 0 }
            val minPoss = possibles.filter { (loads[it] ?: 0) == minLoad }
            val reviewer = minPoss.random()
            assignments[dev]?.add(reviewer)
            loads[reviewer] = (loads[reviewer] ?: 0) + 1
        }

        weeklyEmailService.sendWeeklyEmails(
            assignments = assignments.mapValues { it.value.toList() },
            users = users,
            teamName = team?.name ?: "Unknown Team"
        )

        logger.info("All emails processed at: ${LocalDateTime.now()}")
        System.exit(0)
    }
}