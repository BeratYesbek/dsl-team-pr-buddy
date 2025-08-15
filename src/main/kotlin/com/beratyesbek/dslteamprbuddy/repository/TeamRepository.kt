package com.beratyesbek.dslteamprbuddy.repository

import com.beratyesbek.dslteamprbuddy.entity.Team
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository
import org.springframework.stereotype.Repository

@Repository
interface TeamRepository: FirestoreReactiveRepository<Team> {

}