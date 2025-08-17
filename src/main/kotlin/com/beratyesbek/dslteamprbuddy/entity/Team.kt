package com.beratyesbek.dslteamprbuddy.entity

class Team (
     id: String,
     name: String,
) {
    var id: String = id
        private set
    var name: String = name
        private set

    override fun toString(): String {
        return "Team(id='$id', name='$name')"
    }
}