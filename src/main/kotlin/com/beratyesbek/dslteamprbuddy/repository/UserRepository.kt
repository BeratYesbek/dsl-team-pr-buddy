package com.beratyesbek.dslteamprbuddy.repository

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;

import com.beratyesbek.dslteamprbuddy.entity.User
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface UserRepository : FirestoreReactiveRepository<User> {

    fun findAllByTeamId(teamId: String): Flux<User>
}