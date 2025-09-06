package com.beratyesbek.dslteamprbuddy.service.email.impl

import com.beratyesbek.dslteamprbuddy.entity.EmailMessage
import com.beratyesbek.dslteamprbuddy.entity.EmailSendResult
import com.beratyesbek.dslteamprbuddy.service.email.EmailSender

class RecipientOverrideEmailSender(
    private val delegate: EmailSender,
    private val overrideTo: String
) : EmailSender {
    override fun send(message: EmailMessage): EmailSendResult {
        val overridden = message.copy(
            to = listOf(overrideTo),
            cc = emptyList(),
            bcc = emptyList(),
            subject = "[OVERRIDE-TEST] ${message.subject}"
        )
        return delegate.send(overridden)
    }
}