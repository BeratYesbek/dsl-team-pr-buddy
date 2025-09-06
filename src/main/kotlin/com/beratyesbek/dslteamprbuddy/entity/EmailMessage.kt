package com.beratyesbek.dslteamprbuddy.entity

data class EmailMessage(
    val from: String,
    val to: List<String>,
    val subject: String,
    val htmlBody: String,
    val textBody: String? = null,
    val cc: List<String> = emptyList(),
    val bcc: List<String> = emptyList(),
    val replyTo: String? = null
)