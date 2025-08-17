package com.beratyesbek.dslteamprbuddy.entity

import com.google.cloud.firestore.annotation.DocumentId
import com.google.cloud.firestore.annotation.PropertyName
import com.google.j2objc.annotations.Property
import java.util.UUID

class User(
    @DocumentId
    var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var email: String = "",
    var delta: Int = 0,
    @PropertyName(value = "team_id")
    var teamId : String = "",
    var isAvailable: Boolean = true
) {
    override fun toString(): String {
        return "User(id='$id', name='$name', email='$email', delta=$delta)"
    }
}
