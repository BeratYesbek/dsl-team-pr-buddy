package com.beratyesbek.dslteamprbuddy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class SchedulerApplication {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<SchedulerApplication>(*args)
        }
    }


}