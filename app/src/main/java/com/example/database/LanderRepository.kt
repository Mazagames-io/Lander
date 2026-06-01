package com.example.database

import kotlinx.coroutines.flow.Flow

class LanderRepository(private val dao: LanderDao) {
    val profileFlow: Flow<PilotProfile?> = dao.getProfileFlow()
    val achievementsFlow: Flow<List<Achievement>> = dao.getAchievementsFlow()

    suspend fun getProfile(): PilotProfile {
        var profile = dao.getProfileDirect()
        if (profile == null) {
            profile = PilotProfile()
            dao.updateProfile(profile)
        }
        return profile
    }

    suspend fun updateProfile(profile: PilotProfile) {
        dao.updateProfile(profile)
    }

    suspend fun saveCompletedLanding(
        difficulty: String,
        fuelSavings: Float,
        landingSpeed: Float,
        pointsGained: Int,
        xpGained: Int,
        planetName: String
    ): List<String> {
        val currentProfile = getProfile()
        
        var newXp = currentProfile.xp + xpGained
        var newLevel = currentProfile.level
        val milestones = mutableListOf<String>()
        
        while (newXp >= newLevel * 100) {
            newXp -= newLevel * 100
            newLevel += 1
            milestones.add("Ranked Up! You are now level $newLevel!")
        }

        val updatedProfile = currentProfile.copy(
            xp = newXp,
            level = newLevel,
            totalLandings = currentProfile.totalLandings + 1,
            totalFuelSaved = currentProfile.totalFuelSaved + fuelSavings,
            credits = currentProfile.credits + pointsGained,
            highScore = maxOf(currentProfile.highScore, currentProfile.highScore + pointsGained)
        )
        dao.updateProfile(updatedProfile)

        val unlockedList = mutableListOf<String>()
        val achievements = dao.getAchievementsDirect().associateBy { it.id }.toMutableMap()
        val achToUpdate = mutableListOf<Achievement>()

        fun unlock(id: String) {
            achievements[id]?.let { ach ->
                if (!ach.isUnlocked) {
                    val updated = ach.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis())
                    achToUpdate.add(updated)
                    unlockedList.add(ach.title)
                }
            }
        }

        unlock("first_flight")
        
        if (landingSpeed < 0.5f) {
            unlock("soft_touch")
        }
        
        if (fuelSavings >= 40f) {
            unlock("fuel_saver")
        }
        
        if (difficulty.equals("hard", ignoreCase = true)) {
            unlock("expert_pilot")
        }

        if (newLevel >= 5) {
            unlock("space_explorer")
        }

        if (updatedProfile.credits >= 500) {
            unlock("rich_pilot")
        }

        for (ach in achToUpdate) {
            dao.updateAchievement(ach)
        }

        return unlockedList.map { "Unlocked Achievement: '$it'!" } + milestones
    }

    suspend fun saveFailedLanding(speed: Float): List<String> {
        val currentProfile = getProfile()
        val updatedProfile = currentProfile.copy(
            failedLandings = currentProfile.failedLandings + 1
        )
        dao.updateProfile(updatedProfile)

        val unlockedList = mutableListOf<String>()
        val achievements = dao.getAchievementsDirect()
        
        if (speed > 6.0f) {
            val ach = achievements.find { id -> id.id == "crash_test_dummy" }
            if (ach != null && !ach.isUnlocked) {
                dao.updateAchievement(ach.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis()))
                unlockedList.add(ach.title)
            }
        }

        return unlockedList.map { "Unlocked Achievement: '$it'!" }
    }

    suspend fun purchaseUpgrade(upgradeType: String, cost: Int): Boolean {
        val currentProfile = getProfile()
        if (currentProfile.credits < cost) return false

        val updatedProfile = when (upgradeType) {
            "engine" -> currentProfile.copy(
                credits = currentProfile.credits - cost,
                engineUpgradeLevel = currentProfile.engineUpgradeLevel + 1
            )
            "fuel" -> currentProfile.copy(
                credits = currentProfile.credits - cost,
                fuelTankUpgradeLevel = currentProfile.fuelTankUpgradeLevel + 1
            )
            "thruster" -> currentProfile.copy(
                credits = currentProfile.credits - cost,
                thrusterUpgradeLevel = currentProfile.thrusterUpgradeLevel + 1
            )
            "gear" -> currentProfile.copy(
                credits = currentProfile.credits - cost,
                gearUpgradeLevel = currentProfile.gearUpgradeLevel + 1
            )
            else -> return false
        }
        dao.updateProfile(updatedProfile)
        return true
    }

    suspend fun initializeDbIfNeeded() {
        val profile = dao.getProfileDirect()
        if (profile == null) {
            dao.updateProfile(PilotProfile())
        }
        val achievements = dao.getAchievementsDirect()
        if (achievements.isEmpty()) {
            LanderDatabase.initializeAchievements(dao)
        }
    }

    suspend fun updatePilotName(newName: String) {
        val currentProfile = getProfile()
        dao.updateProfile(currentProfile.copy(name = newName))
    }

    suspend fun resetAllProgress() {
        dao.updateProfile(PilotProfile())
        LanderDatabase.initializeAchievements(dao)
    }
}
