package com.beratyesbek.dslteamprbuddy.service.email

import com.beratyesbek.dslteamprbuddy.entity.EmailMessage
import com.beratyesbek.dslteamprbuddy.entity.EmailSendResult

/**
 * Strategy interface for pluggable email providers (Mailjet, SES, SendGrid, etc.)
 */
fun interface EmailSender {
    fun send(message: EmailMessage): EmailSendResult
}