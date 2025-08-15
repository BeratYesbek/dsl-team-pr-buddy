package com.beratyesbek.dslteamprbuddy.entity

import com.google.cloud.firestore.annotation.DocumentId
import java.util.UUID

class User(
    @DocumentId
    var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var email: String = "",
    var delta: Int = 0,
    var isAvailable: Boolean = true
) {
    override fun toString(): String {
        return "User(id='$id', name='$name', email='$email', delta=$delta)"
    }
}
