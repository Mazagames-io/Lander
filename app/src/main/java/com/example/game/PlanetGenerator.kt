package com.example.game

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class Planet(
    val id: String,
    val name: String,
    val description: String,
    val gravity: Float,
    val windPower: Float,
    val atmosphereDensity: Float,
    val primaryColor: Color,
    val secondaryColor: Color,
    val padWidth: Float,
    val difficulty: String,
    val seed: Int
)

object PlanetGenerator {
    private val PREFIXES = listOf("Aero", "Giga", "Kryo", "Toxis", "Pyro", "Zephyr", "Chronos", "Xeno", "Nox", "Helios")
    private val ROOTS = listOf("Prime", "Nebula", "Dune", "Crater", "Abyss", "Ridge", "Sigma", "Tundra", "Delta")
    private val SUFFIXES = listOf("I", "II", "IV", "IX", "X", "Alpha", "Beta", "Gamma", "Omega")

    fun generateRandomPlanet(difficulty: String): Planet {
        val seed = Random.nextInt(100000)
        val rand = Random(seed)

        val name = "${PREFIXES.random(rand)}-${ROOTS.random(rand)} ${SUFFIXES.random(rand)}"
        
        val (gravity, windPower, atmosphereDensity, primaryColor, secondaryColor, padWidth, description) = when (difficulty) {
            "easy" -> {
                val g = rand.nextDouble(0.07, 0.10).toFloat()
                val w = 0f
                val atm = rand.nextDouble(0.994, 0.997).toFloat()
                val color = Color(0xFF10B981) // Neon mint green
                val secColor = Color(0xFF064E3B)
                val pad = 130f
                val desc = "A temperate greenhouse biosphere. Friendly gravity, supportive atmosphere currents, wide pads."
                LocalPlanetData(g, w, atm, color, secColor, pad, desc)
            }
            "medium" -> {
                val g = rand.nextDouble(0.13, 0.17).toFloat()
                val w = if (rand.nextBoolean()) rand.nextDouble(-0.02, 0.02).toFloat() else 0f
                val atm = rand.nextDouble(0.990, 0.993).toFloat()
                val color = Color(0xFF3B82F6) // Neon azure blue
                val secColor = Color(0xFF1E3A8A)
                val pad = 95f
                val desc = "A cold, frozen ice giant. Regular gravity, persistent horizontal drafts."
                LocalPlanetData(g, w, atm, color, secColor, pad, desc)
            }
            else -> {
                val g = rand.nextDouble(0.19, 0.25).toFloat()
                val w = rand.nextDouble(-0.05, 0.05).toFloat()
                val atm = 1.00f // Vacuum
                val color = Color(0xFFEF4444) // Neon crimson hot list
                val secColor = Color(0xFF7F1D1D)
                val pad = 70f
                val desc = "A hazardous solar volcanic world. Dense fields, massive gravitational pull, compact landing site."
                LocalPlanetData(g, w, atm, color, secColor, pad, desc)
            }
        }

        return Planet(
            id = "procedural_$seed",
            name = name,
            description = description,
            gravity = gravity,
            windPower = windPower,
            atmosphereDensity = atmosphereDensity,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            padWidth = padWidth,
            difficulty = difficulty,
            seed = seed
        )
    }

    private data class LocalPlanetData(
        val gravity: Float,
        val windPower: Float,
        val atmosphereDensity: Float,
        val primaryColor: Color,
        val secondaryColor: Color,
        val padWidth: Float,
        val description: String
    )

    fun getStaticPlanets(): List<Planet> {
        return listOf(
            Planet(
                id = "easy_tutorial",
                name = "Elysium Plains",
                description = "Perfect training planet. Gentle gravity, warm cushioning atmosphere, wide touchdown zone.",
                gravity = 0.07f,
                windPower = 0.0f,
                atmosphereDensity = 0.996f,
                primaryColor = Color(0xFF34D399),
                secondaryColor = Color(0xFF064E3B),
                padWidth = 140f,
                difficulty = "easy",
                seed = 1001
            ),
            Planet(
                id = "medium_glacies",
                name = "Glacies Ridge",
                description = "Glacial glaciers and wind drafts. Moderate gravity, perpetual jet streams.",
                gravity = 0.14f,
                windPower = -0.015f,
                atmosphereDensity = 0.991f,
                primaryColor = Color(0xFF60A5FA),
                secondaryColor = Color(0xFF1E3A8A),
                padWidth = 95f,
                difficulty = "medium",
                seed = 2002
            ),
            Planet(
                id = "hard_infernalis",
                name = "Vulcan Vault",
                description = "Hostile volcanic rift. Extreme gravity, turbulent cross drafts, and narrow landing zones.",
                gravity = 0.23f,
                windPower = 0.04f,
                atmosphereDensity = 1.00f,
                primaryColor = Color(0xFFF87171),
                secondaryColor = Color(0xFF7F1D1D),
                padWidth = 65f,
                difficulty = "hard",
                seed = 3003
            )
        )
    }

    fun generateTerrainPoints(
        width: Float,
        height: Float,
        seed: Int,
        padWidth: Float
    ): TerrainData {
        val rand = Random(seed)
        
        // Constrain pad coordinates between 25% and 70% width
        val padXStart = width * rand.nextDouble(0.25, 0.65).toFloat()
        val padXEnd = padXStart + padWidth
        val padHeightHeight = height * rand.nextDouble(0.68, 0.83).toFloat()

        val points = mutableListOf<Pair<Float, Float>>()
        val numSegments = 50
        val segmentWidth = width / numSegments

        for (i in 0..numSegments) {
            val currX = i * segmentWidth
            
            // If we are in the padding width zone, snap completely flat
            if (currX in padXStart..padXEnd) {
                points.add(Pair(currX, padHeightHeight))
            } else {
                // Procedural generation: smooth noise using sine combinations
                val wave1 = Math.sin(currX.toDouble() * 0.015 + seed) * (height * 0.10)
                val wave2 = Math.cos(currX.toDouble() * 0.005 - seed) * (height * 0.07)
                // Add a small fractal wave
                val wave3 = Math.sin(currX.toDouble() * 0.04 + seed * 2) * (height * 0.02)
                
                var calculatedHeight = (height * 0.72f + wave1 + wave2 + wave3).toFloat()
                
                // Bind elevation constraints
                if (calculatedHeight < height * 0.45f) calculatedHeight = height * 0.45f
                if (calculatedHeight > height * 0.90f) calculatedHeight = height * 0.90f

                points.add(Pair(currX, calculatedHeight))
            }
        }

        return TerrainData(
            points = points,
            padX = padXStart,
            padY = padHeightHeight,
            padWidth = padWidth
        )
    }
}

data class TerrainData(
    val points: List<Pair<Float, Float>>,
    val padX: Float,
    val padY: Float,
    val padWidth: Float
)
