package com.example.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.Achievement
import com.example.database.LanderRepository
import com.example.database.PilotProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface GameState {
    object Idle : GameState
    object Launching : GameState
    object InFlight : GameState
    data class Success(val statsText: String, val earnedCredits: Int, val earnedXp: Int, val alerts: List<String>) : GameState
    data class Crashed(val statsText: String, val speed: Float, val alerts: List<String>) : GameState
}

sealed interface SelectedScreen {
    object Dashboard : SelectedScreen
    object Missions : SelectedScreen
    object Hangar : SelectedScreen
    object Achievements : SelectedScreen
    object FlightSimulator : SelectedScreen
    object Settings : SelectedScreen
}

class LanderViewModel(private val repository: LanderRepository) : ViewModel() {

    // Current screen navigation
    var currentScreen by mutableStateOf<SelectedScreen>(SelectedScreen.Dashboard)
        private set

    // Current game state
    var gameState by mutableStateOf<GameState>(GameState.Idle)
        private set

    // Target selected planet for flight simulation
    var selectedPlanet by mutableStateOf<Planet>(PlanetGenerator.getStaticPlanets().first())
        private set

    // Selected difficulty for missions
    var selectedDifficulty by mutableStateOf<String>("easy")
        private set

    // DB state values
    val pilotProfile: StateFlow<PilotProfile?> = repository.profileFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val achievements: StateFlow<List<Achievement>> = repository.achievementsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Upgrade Shop Cost Configs
    val upgradeBaseCosts = mapOf(
        "engine" to 60,
        "fuel" to 50,
        "thruster" to 40,
        "gear" to 45
    )

    init {
        viewModelScope.launch {
            repository.initializeDbIfNeeded()
            // Default selected planet
            selectedPlanet = PlanetGenerator.getStaticPlanets().first()
        }
    }

    fun navigateTo(screen: SelectedScreen) {
        currentScreen = screen
        if (screen is SelectedScreen.FlightSimulator) {
            gameState = GameState.Launching
        } else {
            gameState = GameState.Idle
        }
    }

    fun selectPlanetAndDifficulty(planet: Planet, difficulty: String) {
        selectedPlanet = planet
        selectedDifficulty = difficulty
    }

    fun startSimulation() {
        gameState = GameState.InFlight
    }

    fun upgradeShip(type: String) {
        val profile = pilotProfile.value ?: return
        val currentLevel = when (type) {
            "engine" -> profile.engineUpgradeLevel
            "fuel" -> profile.fuelTankUpgradeLevel
            "thruster" -> profile.thrusterUpgradeLevel
            "gear" -> profile.gearUpgradeLevel
            else -> 0
        }
        val cost = getUpgradeCost(type, currentLevel)
        if (profile.credits >= cost) {
            viewModelScope.launch {
                repository.purchaseUpgrade(type, cost)
            }
        }
    }

    fun getUpgradeCost(type: String, currentLevel: Int): Int {
        val base = upgradeBaseCosts[type] ?: 50
        return base + (currentLevel * 35)
    }

    // Process landing failure (Crash landing!)
    fun processLanderCrash(verticalSpeed: Float) {
        viewModelScope.launch {
            val alerts = repository.saveFailedLanding(verticalSpeed)
            val formatSpeed = String.format("%.2f", verticalSpeed)
            val statsText = "Your vessel impacted the surface at an unsafe velocity of $formatSpeed m/s. " +
                    "Extreme structural disintegration detected."
            gameState = GameState.Crashed(statsText, verticalSpeed, alerts)
        }
    }

    // Process landing success!
    fun processLanderLanding(remainingFuel: Float, landingSpeed: Float) {
        val profile = pilotProfile.value ?: return
        viewModelScope.launch {
            // Difficulty factors
            val diffMult = when (selectedDifficulty) {
                "easy" -> 1.0f
                "medium" -> 1.5f
                else -> 2.2f
            }

            // Earned logic
            val baseCredits = (50f * diffMult).toInt()
            val baseXP = (50f * diffMult).toInt()

            // Fuel bonuses
            val fuelEfficiencyBonus = (remainingFuel * 0.75f * diffMult).toInt()
            
            // Soft touchdown bonuses
            val precisionBonus = if (landingSpeed < 0.5f) 40 else 0

            val totalCreditsEarned = baseCredits + fuelEfficiencyBonus + precisionBonus
            val totalXpEarned = baseXP + (fuelEfficiencyBonus / 2) + precisionBonus

            val alerts = repository.saveCompletedLanding(
                difficulty = selectedDifficulty,
                fuelSavings = remainingFuel,
                landingSpeed = landingSpeed,
                pointsGained = totalCreditsEarned,
                xpGained = totalXpEarned,
                planetName = selectedPlanet.name
            )

            val formatRemFuel = String.format("%.1f", remainingFuel)
            val formatSpeed = String.format("%.2f", landingSpeed)
            val statsText = "Perfect Landing Touchdown!\n" +
                    "• Vertical velocity: $formatSpeed m/s (SAFE)\n" +
                    "• Remaining thruster fuel: $formatRemFuel Liters\n" +
                    "• Base Award: +$baseCredits Units\n" +
                    "• Fuel Eco Reward: +$fuelEfficiencyBonus Units\n" +
                    "• Touchdown Precision Bonus: +$precisionBonus Units"

            gameState = GameState.Success(
                statsText = statsText,
                earnedCredits = totalCreditsEarned,
                earnedXp = totalXpEarned,
                alerts = alerts
            )
        }
    }

    // Get spaceship specs based on upgrades
    fun getEngineBurnRate(level: Int): Float {
        // Base burn rate is 0.16; decrease by 0.015 per level (limit to 0.06 min)
        return maxOf(0.06f, 0.16f - (level * 0.018f))
    }

    fun getMaxFuel(level: Int): Float {
        // Base is 100 liters; increase by 15 per level
        return 100f + (level * 20f)
    }

    fun getReactionThrusterRotationRate(level: Int): Float {
        // Base is 2.5 degrees per frame; increase by 0.5 per level
        return 2.5f + (level * 0.5f)
    }

    fun getSafeLandingSpeedThreshold(level: Int): Float {
        // Base is 1.8f m/s; increase by 0.25f per level
        return 1.8f + (level * 0.25f)
    }

    fun updatePilotName(newName: String) {
        viewModelScope.launch {
            repository.updatePilotName(newName)
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            repository.resetAllProgress()
        }
    }
}
