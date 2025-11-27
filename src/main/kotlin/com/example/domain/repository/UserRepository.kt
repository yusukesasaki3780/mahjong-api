package com.example.domain.repository

import com.example.domain.model.GameType
import com.example.domain.model.RankingEntry
import com.example.domain.model.StatsPeriod
import com.example.domain.model.User

/**
 * Repository contract for user persistence and ranking retrieval.
 */
interface UserRepository {

    suspend fun findById(userId: Long): User?

    suspend fun findByEmail(email: String): User?

    suspend fun createUser(user: User): User

    suspend fun updateUser(user: User): User

    suspend fun patchUser(userId: Long, patch: UserPatch): User

    suspend fun deleteUser(userId: Long): Boolean

    /**
     * Returns aggregated ranking entries filtered by game type and period.
     */
    suspend fun findRanking(gameType: GameType, period: StatsPeriod): List<RankingEntry>
}
