package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pilot_profile")
data class PilotProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Cadet Pilot",
    val xp: Int = 0,
    val level: Int = 1,
    val totalLandings: Int = 0,
    val failedLandings: Int = 0,
    val totalFuelSaved: Float = 0f,
    val highScore: Int = 0,
    val credits: Int = 100,
    val engineUpgradeLevel: Int = 0,
    val fuelTankUpgradeLevel: Int = 0,
    val thrusterUpgradeLevel: Int = 0,
    val gearUpgradeLevel: Int = 0
)
