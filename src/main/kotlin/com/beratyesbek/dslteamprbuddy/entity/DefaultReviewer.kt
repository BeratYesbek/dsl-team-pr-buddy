package com.beratyesbek.dslteamprbuddy.entity

import com.google.cloud.firestore.annotation.DocumentId
import java.util.UUID

class DefaultReviewer(
    var devName: String = "",
    var reviewerName: String = ""
) {
    @DocumentId
    var id: String = UUID.randomUUID().toString()
        private set

    override fun toString(): String {
        return "DefaultReviewer(id='$id', devName='$devName', reviewerName='$reviewerName')"
    }
}
