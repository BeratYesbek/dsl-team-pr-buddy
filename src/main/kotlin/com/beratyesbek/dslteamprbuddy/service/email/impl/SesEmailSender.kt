package com.beratyesbek.dslteamprbuddy.service.email.impl

import com.beratyesbek.dslteamprbuddy.entity.EmailMessage
import com.beratyesbek.dslteamprbuddy.entity.EmailSendResult
import com.beratyesbek.dslteamprbuddy.service.email.EmailSender
import org.slf4j.LoggerFactory

class SesEmailSender : EmailSender {
    private val log = LoggerFactory.getLogger(SesEmailSender::class.java)
    override fun send(message: EmailMessage) =
        EmailSendResult(provider = "ses", ok = false, error = "Not implemented")
}