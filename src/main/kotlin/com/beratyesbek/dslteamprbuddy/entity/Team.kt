package com.beratyesbek.dslteamprbuddy.entity

class Team (
     var id: String = "",
     var name: String= "",
) {

    override fun toString(): String {
        return "Team(id='$id', name='$name')"
    }
}