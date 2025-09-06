package com.beratyesbek.dslteamprbuddy.entity

data class EmailSendResult(
    val provider: String,
    val messageId: String? = null,
    val ok: Boolean = true,
    val error: String? = null
)