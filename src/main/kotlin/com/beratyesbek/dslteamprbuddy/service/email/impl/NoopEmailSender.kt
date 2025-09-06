package com.beratyesbek.dslteamprbuddy.service.email.impl

import com.beratyesbek.dslteamprbuddy.entity.EmailMessage
import com.beratyesbek.dslteamprbuddy.entity.EmailSendResult
import com.beratyesbek.dslteamprbuddy.service.email.EmailSender
import org.slf4j.LoggerFactory

class NoopEmailSender : EmailSender {
    private val log = LoggerFactory.getLogger(NoopEmailSender::class.java)

    override fun send(message: EmailMessage): EmailSendResult {
        log.info(
            "NOOP email -> to={}, subject={}, body(length)={}",
            message.to.joinToString(), message.subject, message.htmlBody.length
        )
        return EmailSendResult(provider = "noop", messageId = null, ok = true)
    }
}