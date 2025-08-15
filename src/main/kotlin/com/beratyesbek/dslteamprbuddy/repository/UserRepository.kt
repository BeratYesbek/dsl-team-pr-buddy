package com.beratyesbek.dslteamprbuddy.repository
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;

import com.beratyesbek.dslteamprbuddy.entity.User
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : FirestoreReactiveRepository<User>  {
}