package com.beratyesbek.dslteamprbuddy.entity

import com.google.cloud.firestore.annotation.DocumentId
import java.util.UUID

class User(
    @DocumentId
    var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var email: String = "",
    var delta: Int = 0,
    var teamId : String = "",
    var isAvailable: Boolean = true,
    var mainLanguage: String = ""
) {
    override fun toString(): String {
        return "User(id='$id', name='$name', email='$email', delta=$delta)"
    }
}
