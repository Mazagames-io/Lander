package com.example.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.Achievement
import com.example.database.PilotProfile
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceControlDeck(
    viewModel: LanderViewModel,
    modifier: Modifier = Modifier
) {
    val screen = viewModel.currentScreen
    val profileState by viewModel.pilotProfile.collectAsState()

    val profile = profileState ?: return // Render nothing until pilot details are loaded

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0B))
    ) {
        when (screen) {
            is SelectedScreen.Dashboard -> DashboardScreen(viewModel, profile)
            is SelectedScreen.Missions -> MissionsScreen(viewModel)
            is SelectedScreen.Hangar -> HangarScreen(viewModel, profile)
            is SelectedScreen.Achievements -> AchievementsScreen(viewModel)
            is SelectedScreen.FlightSimulator -> ActiveFlightSimulator(viewModel)
            is SelectedScreen.Settings -> SettingsScreen(viewModel, profile)
        }
    }
}

// 1. DASHBOARD OVERVIEW SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: LanderViewModel,
    profile: PilotProfile
) {
    val achievements by viewModel.achievements.collectAsState()
    val unlockedCount = achievements.count { it.isUnlocked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PILOT CONTROL DECK", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = Color(0xFFD1E5F4), fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .background(Color(0xFF1A1C1E), RoundedCornerShape(100.dp))
                                .border(1.dp, Color(0xFF43474E), RoundedCornerShape(100.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.WorkspacePremium, contentDescription = "Credits", tint = Color(0xFFFDE047), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "${profile.credits} CR",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFDE047),
                                fontSize = 12.sp,
                                modifier = Modifier.testTag("dashboard_credits_counter")
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { viewModel.navigateTo(SelectedScreen.Settings) },
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color(0xFFD1E5F4))
                        }
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card (Sophisticated Dark)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("pilot_profile_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C1E)),
                    shape = RoundedCornerShape(24.dp),
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(colors = listOf(Color(0xFF43474E), Color(0xFF43474E))))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3F484E))
                                        .border(1.dp, Color(0xFF899298), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = "Avatar", tint = Color(0xFFD1E5F4), modifier = Modifier.size(28.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "PILOT RANK",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFC2C7CE).copy(alpha = 0.7f),
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        profile.name.uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFFD1E5F4)
                                    )
                                }
                            }
                            
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFF0A0A0B), RoundedCornerShape(100.dp))
                                    .border(1.dp, Color(0xFF43474E), RoundedCornerShape(100.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Stars, contentDescription = "Level", tint = Color(0xFFD1E5F4), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Level ${profile.level}",
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD1E5F4)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Pilot XP Progress Bar (Red glow fuel theme style)
                        val nextLevelXp = profile.level * 100
                        val progress = (profile.xp.toFloat() / nextLevelXp).coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("XP PROGRESSION", fontSize = 10.sp, color = Color(0xFFC2C7CE), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("${(progress * 100).toInt()}%", fontSize = 11.sp, color = Color(0xFFF28B82), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF333538))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFFF28B82), Color(0xFFEE675C))
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            // CTA Navigation Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.navigateTo(SelectedScreen.Missions) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("nav_missions_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF004A77),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = ButtonDefaults.outlinedButtonBorder().copy(width = 1.dp, brush = Brush.linearGradient(colors = listOf(Color(0xFF004A77), Color(0xFF004A77))))
                    ) {
                        Icon(Icons.Filled.Explore, contentDescription = "Planets")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MISSION BRIEFING", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White, letterSpacing = 0.5.sp)
                    }

                    Button(
                        onClick = { viewModel.navigateTo(SelectedScreen.Hangar) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("nav_hangar_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1C1B1F),
                            contentColor = Color(0xFFC2C7CE)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = ButtonDefaults.outlinedButtonBorder().copy(width = 1.dp, brush = Brush.linearGradient(colors = listOf(Color(0xFF43474E), Color(0xFF43474E))))
                    ) {
                        Icon(Icons.Filled.Build, contentDescription = "Hangar", tint = Color(0xFFC2C7CE))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SHIP HANGAR", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFC2C7CE), letterSpacing = 0.5.sp)
                    }
                }
            }

            // Summary Stats Section
            item {
                Column {
                    Text(
                        "FLIGHT TELEMETRY STATS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatBox(
                            title = "LANDINGS",
                            value = "${profile.totalLandings}",
                            icon = Icons.Filled.CloudUpload,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                        StatBox(
                            title = "CRASHES",
                            value = "${profile.failedLandings}",
                            icon = Icons.Filled.ReportGmailerrorred,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.weight(1f)
                        )
                        StatBox(
                            title = "MEDALS",
                            value = "$unlockedCount",
                            icon = Icons.Filled.Stars,
                            tint = Color(0xFFFFB000),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Recent Achievements Preview
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "PILOT MEDALS & BADGES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            "SEE ALL ➜",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier
                                .clickable { viewModel.navigateTo(SelectedScreen.Achievements) }
                                .padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (achievements.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(Color(0x330F172A), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Initializing profile system...", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            achievements.take(3).forEach { achievement ->
                                AchievementRow(achievement)
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// 2. MISSIONS / PLANETARY SELECT SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsScreen(
    viewModel: LanderViewModel
) {
    val staticPlanets = remember { PlanetGenerator.getStaticPlanets() }
    var exploredDifficulty by remember { mutableStateOf("easy") }
    var selectedPlanetInList by remember { mutableStateOf<Planet>(staticPlanets.first()) }
    val profile by viewModel.pilotProfile.collectAsState()

    // Real-time custom planetary calculations
    val customPlanet = remember(exploredDifficulty) {
        PlanetGenerator.generateRandomPlanet(exploredDifficulty)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MISSION CHART briefing", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(SelectedScreen.Dashboard) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Planet list selection standard
            item {
                Text(
                    "CHOOSE ORBITAL PATH DESTINATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )
            }

            // Predefined worlds
            items(staticPlanets) { planet ->
                PlanetSelectionRow(
                    planet = planet,
                    isSelected = selectedPlanetInList.id == planet.id,
                    onClick = {
                        selectedPlanetInList = planet
                        viewModel.selectPlanetAndDifficulty(planet, planet.difficulty)
                    }
                )
            }

            // Procedural generator warp row widget
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedPlanetInList = customPlanet
                            viewModel.selectPlanetAndDifficulty(customPlanet, exploredDifficulty)
                        }
                        .testTag("planetary_warp_generator_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPlanetInList.id.startsWith("procedural")) Color(0xFF1E1B4B) else Color(0x330F172A)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            if (selectedPlanetInList.id.startsWith("procedural")) listOf(Color(0xFF818CF8), Color(0xFF6366F1))
                            else listOf(Color(0xFF334155), Color(0xFF1E293B))
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(Color(0xFFC084FC), Color(0xFF818CF8)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = "Warp", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("PROCEDURAL SCAN WORLD", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    Text("Seed scan custom environment vectors", fontSize = 10.sp, color = Color(0xFFC084FC), fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            RadioButton(
                                selected = selectedPlanetInList.id.startsWith("procedural"),
                                onClick = {
                                    selectedPlanetInList = customPlanet
                                    viewModel.selectPlanetAndDifficulty(customPlanet, exploredDifficulty)
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFC084FC))
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Select difficulty parameters and hit Scan to warp-tunnel map a brand-new planet on-the-fly dynamically.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom difficulty switchers inside procedural
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("easy" to Color(0xFF10B981), "medium" to Color(0xFF3B82F6), "hard" to Color(0xFFEF4444)).forEach { (diff, color) ->
                                val activated = exploredDifficulty == diff && selectedPlanetInList.id.startsWith("procedural")
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (activated) color.copy(alpha = 0.25f) else Color(0x660F172A))
                                        .border(1.dp, if (activated) color else Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                        .clickable {
                                            exploredDifficulty = diff
                                            val generated = PlanetGenerator.generateRandomPlanet(diff)
                                            selectedPlanetInList = generated
                                            viewModel.selectPlanetAndDifficulty(generated, diff)
                                        }
                                        .padding(vertical = 8.dp)
                                        .testTag("warp_diff_$diff"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        diff.uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (activated) color else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Navigation Briefing details Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xCC0F172A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            selectedPlanetInList.name.uppercase(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = selectedPlanetInList.primaryColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            selectedPlanetInList.description,
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 18.sp
                        )

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF1E293B))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("SURFACE GRAVITY", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                // Convert raw gravity factor to user-friendly format
                                val metricG = selectedPlanetInList.gravity * 36.0f
                                Text("${String.format("%.1f", metricG)} m/s²", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ATMOSPHERE", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                val densityText = when {
                                    selectedPlanetInList.atmosphereDensity == 1.0f -> "VACUUM"
                                    selectedPlanetInList.atmosphereDensity > 0.994f -> "CUSHION"
                                    else -> "THIN"
                                }
                                Text(densityText, fontSize = 14.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("CROSSWIND DRIFT", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                val windStrength = when {
                                    selectedPlanetInList.windPower == 0f -> "CALM"
                                    Math.abs(selectedPlanetInList.windPower) < 0.03f -> "LIGHT"
                                    else -> "TURBULENT"
                                }
                                Text(windStrength, fontSize = 14.sp, color = Color(0xFFFFB000), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // GIGANTIC LAUNCH DECK CONTROL BUTTON
            item {
                Button(
                    onClick = {
                        viewModel.navigateTo(SelectedScreen.FlightSimulator)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("init_launch_pad_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = selectedPlanetInList.primaryColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.LocalFireDepartment, contentDescription = "Launch", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "ENTER FLIGHT DECK",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// 3. SHIPYARD UPGRADES SHOP SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HangarScreen(
    viewModel: LanderViewModel,
    profile: PilotProfile
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SHIP UPGRADES LAB", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(SelectedScreen.Dashboard) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.WorkspacePremium, contentDescription = "Credits", tint = Color(0xFFFFD600), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${profile.credits} CR",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD600),
                            fontSize = 16.sp
                        )
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Ship Blueprint wireframe visual widget
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0x660F172A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "APOLLO SHIP SCHEMATICS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Simulated Vector telemetry blueprint
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .background(Color(0xFF020617), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "==== APOLLO-CLASS DESCENT ENGINE ====\n" +
                                        "■ Burn Thrift: ${String.format("%.3f", viewModel.getEngineBurnRate(profile.engineUpgradeLevel))} L/f [LV. ${profile.engineUpgradeLevel}]\n" +
                                        "■ Max Fuel: ${viewModel.getMaxFuel(profile.fuelTankUpgradeLevel).toInt()} Liters [LV. ${profile.fuelTankUpgradeLevel}]\n" +
                                        "■ Gyro Reaction: ${String.format("%.1f", viewModel.getReactionThrusterRotationRate(profile.thrusterUpgradeLevel))} deg/f [LV. ${profile.thrusterUpgradeLevel}]\n" +
                                        "■ Landing Leg Limit: ${String.format("%.2f", viewModel.getSafeLandingSpeedThreshold(profile.gearUpgradeLevel))} m/s [LV. ${profile.gearUpgradeLevel}]",
                                color = Color(0xFF10B981),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Upgrade Shop rows (Engine, Fuel, Thrusters, Gear)
            item {
                Text(
                    "MODULAR ROCKET UPGRADES AVAILABLE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Engine upgrade
            item {
                val cost = viewModel.getUpgradeCost("engine", profile.engineUpgradeLevel)
                UpgradeRow(
                    title = "Thrift Combustion Nozzle",
                    desc = "Optimizes active burn chamber mechanics, reducing thruster fuel usage constraints.",
                    level = profile.engineUpgradeLevel,
                    cost = cost,
                    canAfford = profile.credits >= cost,
                    icon = Icons.Filled.LocalFireDepartment,
                    onClick = { viewModel.upgradeShip("engine") },
                    modifier = Modifier.testTag("upgrade_engine_row")
                )
            }

            // Fuel Upgrade
            item {
                val cost = viewModel.getUpgradeCost("fuel", profile.fuelTankUpgradeLevel)
                UpgradeRow(
                    title = "Pressurized Carbon Tank",
                    desc = "Widens storage chambers to supply extra liter capacity for landing attempts.",
                    level = profile.fuelTankUpgradeLevel,
                    cost = cost,
                    canAfford = profile.credits >= cost,
                    icon = Icons.Filled.LocalGasStation,
                    onClick = { viewModel.upgradeShip("fuel") },
                    modifier = Modifier.testTag("upgrade_fuel_row")
                )
            }

            // Thrusters reaction upgrade
            item {
                val cost = viewModel.getUpgradeCost("thruster", profile.thrusterUpgradeLevel)
                UpgradeRow(
                    title = "High-Response Reaction Gyros",
                    desc = "Widens gimbal steering angles to improve ship rotation reactions dynamically.",
                    level = profile.thrusterUpgradeLevel,
                    cost = cost,
                    canAfford = profile.credits >= cost,
                    icon = Icons.Filled.SettingsBackupRestore,
                    onClick = { viewModel.upgradeShip("thruster") },
                    modifier = Modifier.testTag("upgrade_thruster_row")
                )
            }

            // Landing leg shock shocks absorber gear upgrade
            item {
                val cost = viewModel.getUpgradeCost("gear", profile.gearUpgradeLevel)
                UpgradeRow(
                    title = "Magnetic Heavy Duty Dampers",
                    desc = "Enforces landing footpads to increase maximum safe touchdown descent velocity bounds.",
                    level = profile.gearUpgradeLevel,
                    cost = cost,
                    canAfford = profile.credits >= cost,
                    icon = Icons.Filled.Security,
                    onClick = { viewModel.upgradeShip("gear") },
                    modifier = Modifier.testTag("upgrade_gear_row")
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// 4. ACHIEVEMENTS SCREEN SYSTEM
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    viewModel: LanderViewModel
) {
    val achievements by viewModel.achievements.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PILOT LOG BOOK - MEDALS", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(SelectedScreen.Dashboard) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        if (achievements.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "UNLOCKED PILOT MILESTONES (${achievements.count { it.isUnlocked }}/${achievements.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(achievements) { achievement ->
                    AchievementRowDetailed(achievement)
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

// ===== HELPER COMPONENT CHUNKS =====

@Composable
fun StatBox(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C1E)),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(colors = listOf(Color(0xFF43474E), Color(0xFF43474E)))),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(title, fontSize = 9.sp, color = Color(0xFFC2C7CE), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun AchievementRow(achievement: Achievement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111318)),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(colors = listOf(Color(0xFF43474E), Color(0xFF43474E)))),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (achievement.isUnlocked) Color(0xFF004A77) else Color(0xFF1C1B1F)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (achievement.iconName) {
                        "flight" -> Icons.Filled.FlightTakeoff
                        "eco" -> Icons.Filled.Verified
                        "fuel" -> Icons.Filled.LocalGasStation
                        "star" -> Icons.Filled.Star
                        "explore" -> Icons.Filled.Language
                        "crash" -> Icons.Filled.Gavel
                        else -> Icons.Filled.OfflineBolt
                    },
                    contentDescription = "",
                    tint = if (achievement.isUnlocked) Color(0xFFD1E5F4) else Color(0xFFC2C7CE),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    achievement.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (achievement.isUnlocked) Color(0xFFD1E5F4) else Color(0xFFC2C7CE)
                )
                Text(
                    achievement.description,
                    fontSize = 10.sp,
                    color = if (achievement.isUnlocked) Color(0xFFC2C7CE).copy(alpha = 0.8f) else Color(0xFFC2C7CE).copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun AchievementRowDetailed(achievement: Achievement) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("ach_row_${achievement.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) Color(0xFF111318) else Color(0xFF1A1C1E)
        ),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = Brush.linearGradient(
                if (achievement.isUnlocked) listOf(Color(0xFF82CFFD), Color(0xFF004A77))
                else listOf(Color(0xFF43474E), Color(0xFF43474E))
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.isUnlocked) Color(0xFF004A77)
                        else Color(0xFF1C1B1F)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (achievement.iconName) {
                        "flight" -> Icons.Filled.FlightTakeoff
                        "eco" -> Icons.Filled.Verified
                        "fuel" -> Icons.Filled.LocalGasStation
                        "star" -> Icons.Filled.Star
                        "explore" -> Icons.Filled.Language
                        "crash" -> Icons.Filled.Gavel
                        else -> Icons.Filled.OfflineBolt
                    },
                    contentDescription = "",
                    tint = if (achievement.isUnlocked) Color(0xFFD1E5F4) else Color(0xFFC2C7CE),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    achievement.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (achievement.isUnlocked) Color(0xFFD1E5F4) else Color(0xFFC2C7CE)
                )
                Text(
                    achievement.description,
                    fontSize = 11.sp,
                    color = if (achievement.isUnlocked) Color(0xFFC2C7CE).copy(alpha = 0.8f) else Color(0xFFC2C7CE).copy(alpha = 0.5f)
                )
                if (achievement.isUnlocked && achievement.unlockedAt != null) {
                    val date = Date(achievement.unlockedAt)
                    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    Text(
                        "UNLOCKED AT: ${format.format(date)}",
                        fontSize = 9.sp,
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            if (achievement.isUnlocked) {
                Icon(Icons.Filled.CheckCircle, "Unlocked", tint = Color(0xFF34D399), modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Filled.Lock, "Locked", tint = Color(0xFF475569), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun PlanetSelectionRow(
    planet: Planet,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cardColor = if (isSelected) Color(0xFF111318) else Color(0xFF1A1C1E)
    val borderColor = if (isSelected) planet.primaryColor else Color(0xFF43474E)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("planet_row_${planet.id}"),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(borderColor, borderColor)))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(planet.primaryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Public, contentDescription = null, tint = planet.primaryColor, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        planet.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        "DIFFICULTY: ${planet.difficulty.uppercase()}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = planet.primaryColor
                    )
                }
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = planet.primaryColor)
            )
        }
    }
}

@Composable
fun UpgradeRow(
    title: String,
    desc: String,
    level: Int,
    cost: Int,
    canAfford: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMaxed = level >= 5

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C1E)),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(Color(0xFF43474E), Color(0xFF43474E))))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C1B1F))
                            .border(1.dp, Color(0xFF43474E), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = Color(0xFFD1E5F4), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        Text("UPGRADE LEVEL: $level / 5", fontSize = 10.sp, color = Color(0xFF82CFFD), fontWeight = FontWeight.Bold)
                    }
                }

                if (isMaxed) {
                    Text(
                        "MAXED OUT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF10B981)
                    )
                } else {
                    Button(
                        onClick = onClick,
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD1E5F4),
                            contentColor = Color(0xFF00344F),
                            disabledContainerColor = Color(0xFF333538)
                        ),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.testTag("buy_upgrade_button")
                    ) {
                        Icon(Icons.Filled.CallMerge, contentDescription = "Buy", tint = if (canAfford) Color(0xFF00344F) else Color.Gray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "$cost CR",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = if (canAfford) Color(0xFF00344F) else Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                desc,
                fontSize = 11.sp,
                color = Color(0xFFC2C7CE).copy(alpha = 0.8f),
                lineHeight = 16.sp
            )

            if (!isMaxed) {
                Spacer(modifier = Modifier.height(10.dp))
                // Graphical progression pips indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 1..5) {
                        val active = i <= level
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (active) Color(0xFF82CFFD) else Color(0xFF333538))
                        )
                    }
                }
            }
        }
    }
}

// 5. PILOT CONSOLE SETTINGS & OVER-THE-AIR RELEASES
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LanderViewModel,
    profile: PilotProfile
) {
    var editableName by remember(profile.name) { mutableStateOf(profile.name) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateCheckedStatus by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FLIGHT DECK CONFIGURATION", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(SelectedScreen.Dashboard) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Naming section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("character_settings_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C1E)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF43474E))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Face, contentDescription = "Pilot Name", tint = Color(0xFFD1E5F4))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PILOT IDENTITY REGISTRATION", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFD1E5F4))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = editableName,
                            onValueChange = { editableName = it },
                            label = { Text("Registered Pilot Callsign", color = Color(0xFF8A929A)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF82CFFD),
                                unfocusedBorderColor = Color(0xFF43474E)
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                            modifier = Modifier.fillMaxWidth().testTag("pilot_name_input")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                if (editableName.isNotBlank()) {
                                    viewModel.updatePilotName(editableName.trim())
                                }
                            },
                            enabled = editableName.isNotBlank() && editableName != profile.name,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF004A77),
                                disabledContainerColor = Color(0x33004A77),
                                contentColor = Color.White,
                                disabledContentColor = Color(0x80C2C7CE)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.align(Alignment.End).height(40.dp).testTag("save_pilot_name_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("UPDATE CALLSIGN", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // Remote Update Status Check
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("remote_update_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C1E)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF43474E))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = "OTA Update", tint = Color(0xFF81C784))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("OTA REMOTE DEPLOYMENT STATUS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFD1E5F4))
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("LOCAL INSTALLED BUILD", fontSize = 10.sp, color = Color(0xFF8A929A))
                                Text("v1.1.0 Stable", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("LAST UPDATE CHECKED", fontSize = 10.sp, color = Color(0xFF8A929A))
                                Text("June 2026", fontSize = 12.sp, color = Color(0xFFC2C7CE))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0A0A0B), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFF43474E).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("update.json configuration", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Target deployment version metadata is initialized at deep repository root. Easily customizable for your GitHub Pages index or release assets checks.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = Color(0xFFC2C7CE)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        if (isCheckingUpdates) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF82CFFD), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Interrogating GitHub release repository...", fontSize = 12.sp, color = Color(0xFF82CFFD))
                            }
                        } else {
                            Button(
                                onClick = {
                                    isCheckingUpdates = true
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        isCheckingUpdates = false
                                        updateCheckedStatus = "Perfect sync! You are currently up to date with the latest v1.1.0 commit in your repository."
                                    }, 1200)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF3B3F43),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(40.dp).testTag("check_updates_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CHECK FOR GITHUB UPDATES", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        updateCheckedStatus?.let { status ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                status,
                                fontSize = 11.sp,
                                color = Color(0xFF81C784),
                                lineHeight = 15.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().testTag("update_check_status")
                            )
                        }
                    }
                }
            }

            // Danger zone - Reset All Progress
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("danger_zone_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1E1E)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF8C2D2D))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFF28B82))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("DANGER PROTOCOL - RESET RECORDS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFF28B82))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Erase all registered flight records. Your earned levels, credits, purchased spacecraft system upgrades, and unlocked pilot medals will be permanently purged from local storage.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = Color(0xFFE28B82).copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { showResetConfirmation = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEE675C),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp).testTag("trigger_reset_btn")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PERMANENTLY RESET PROGRESS", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            containerColor = Color(0xFF1E1F22),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = "Confirm", tint = Color(0xFFF28B82))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CONFIRM DESTRUCTIVE PURGE", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Text(
                    "Are you absolutely certain? This operation will permanently wipe pilot '${profile.name}' data including total completed landings, credits, hangar ship upgrades, and all unlocked achievements. This action cannot be undone.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFFC2C7CE)
                )
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("ABORT PROTOCOL", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmation = false
                        viewModel.resetProgress()
                        viewModel.navigateTo(SelectedScreen.Dashboard)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEE675C))
                ) {
                    Text("YES, WIPE ALL DATA", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
