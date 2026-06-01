package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LanderDao {
    @Query("SELECT * FROM pilot_profile WHERE id = 1")
    fun getProfileFlow(): Flow<PilotProfile?>

    @Query("SELECT * FROM pilot_profile WHERE id = 1")
    suspend fun getProfileDirect(): PilotProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProfile(profile: PilotProfile)

    @Query("SELECT * FROM achievements")
    fun getAchievementsFlow(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements")
    suspend fun getAchievementsDirect(): List<Achievement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<Achievement>)

    @Update
    suspend fun updateAchievement(achievement: Achievement)
}
