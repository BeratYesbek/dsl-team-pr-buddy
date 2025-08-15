package com.example.scheduler

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.time.LocalDateTime

@SpringBootApplication
class SchedulerApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<SchedulerApplication>(*args)
        }
    }

    @Bean
    fun taskRunner() = ApplicationRunner { _: ApplicationArguments ->
        val logger = LoggerFactory.getLogger(SchedulerApplication::class.java)
        logger.info("Scheduled task executed at: ${LocalDateTime.now()}")
        System.exit(0)
    }
}