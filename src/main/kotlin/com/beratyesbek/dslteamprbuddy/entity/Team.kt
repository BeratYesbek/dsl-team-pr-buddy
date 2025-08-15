package com.beratyesbek.dslteamprbuddy.entity

class Team (
     id: String,
     name: String,
     members: List<String>
) {

    var id: String = id
        private set
    var name: String = name
        private set
    var members: List<String> = members
        private set


    override fun toString(): String {
        return "Team(name='$name', members=$members"
    }
}