package com.example.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Smoke particle system
data class ThrusterParticle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    var age: Int,
    val maxAge: Int,
    val color: Color,
    val size: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveFlightSimulator(
    viewModel: LanderViewModel,
    modifier: Modifier = Modifier
) {
    val planet = viewModel.selectedPlanet
    val difficulty = viewModel.selectedDifficulty
    val profile by viewModel.pilotProfile.collectAsState()
    
    // Spaceship capabilities derived from upgrades
    val engineLevel = profile?.engineUpgradeLevel ?: 0
    val fuelLevel = profile?.fuelTankUpgradeLevel ?: 0
    val thrusterLevel = profile?.thrusterUpgradeLevel ?: 0
    val gearLevel = profile?.gearUpgradeLevel ?: 0

    val maxFuel = viewModel.getMaxFuel(fuelLevel)
    val burnRate = viewModel.getEngineBurnRate(engineLevel)
    val rotationRate = viewModel.getReactionThrusterRotationRate(thrusterLevel)
    val safeSpeedLimit = viewModel.getSafeLandingSpeedThreshold(gearLevel)

    // Layout configuration state
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }
    
    // Physics simulation state variables
    var posX by remember { mutableStateOf(0f) }
    var posY by remember { mutableStateOf(0f) }
    var velX by remember { mutableStateOf(0f) }
    var velY by remember { mutableStateOf(0f) }
    var angle by remember { mutableStateOf(0f) } // 0 is straight up, +ve is right tilt, -ve is left tilt
    var fuel by remember { mutableStateOf(maxFuel) }
    
    // Interactive controls trigger state
    var isPushingThrust by remember { mutableStateOf(false) }
    var isRotatingLeft by remember { mutableStateOf(false) }
    var isRotatingRight by remember { mutableStateOf(false) }
    var isAutoLanderActive by remember { mutableStateOf(false) }

    // Visual camera shake factor (pixels offset on crash/thrust)
    var cameraShakeX by remember { mutableStateOf(0f) }
    var cameraShakeY by remember { mutableStateOf(0f) }

    // Particle trace
    val particles = remember { mutableStateListOf<ThrusterParticle>() }

    // High fidelity environment
    val terrainData = remember(canvasWidth, canvasHeight, planet) {
        if (canvasWidth > 0f && canvasHeight > 0f) {
            PlanetGenerator.generateTerrainPoints(canvasWidth, canvasHeight, planet.seed, planet.padWidth)
        } else {
            null
        }
    }

    // Twinkling background stars
    val stars = remember(canvasWidth, canvasHeight) {
        if (canvasWidth > 0f && canvasHeight > 0f) {
            List(60) {
                Offset(Random.nextFloat() * canvasWidth, Random.nextFloat() * canvasHeight)
            }
        } else {
            emptyList()
        }
    }

    // Reset physics when launching/entering Inflight
    LaunchedEffect(viewModel.gameState, terrainData) {
        if (viewModel.gameState == GameState.Launching && canvasWidth > 0f) {
            posX = canvasWidth / 2f
            posY = canvasHeight * 0.15f
            // Easy has no initial momentum, others have small drift
            velX = if (difficulty == "easy") 0f else Random.nextDouble(-1.2, 1.2).toFloat()
            velY = 0.5f
            angle = 0f
            fuel = maxFuel
            particles.clear()
            isPushingThrust = false
            isRotatingLeft = false
            isRotatingRight = false
            isAutoLanderActive = false
        }
    }

    // Active ticking game loop (approx 60 fps)
    LaunchedEffect(viewModel.gameState) {
        if (viewModel.gameState == GameState.InFlight) {
            while (viewModel.gameState == GameState.InFlight) {
                // Read specs reactively
                val engineThrust = 0.28f // Thrust push per frame
                val dragDensity = planet.atmosphereDensity // e.g. 0.995f

                // 1. Auto Lander Autopilot Logic
                if (isAutoLanderActive && fuel > 0f) {
                    val padCenter = (terrainData?.padX ?: 0f) + ((terrainData?.padWidth ?: 100f) / 2f)
                    val dx = padCenter - posX
                    val currentTerrainY = interpolateTerrainY(posX, terrainData?.points ?: emptyList())
                    val dy = currentTerrainY - posY

                    // Level the craft completely for ideal landing posture
                    if (angle > 0f) {
                        angle = (angle - 1.5f).coerceAtLeast(0f)
                    } else if (angle < 0f) {
                        angle = (angle + 1.5f).coerceAtMost(0f)
                    }

                    // Centering navigation
                    if (dx > 12f) {
                        isRotatingRight = true
                        isRotatingLeft = false
                    } else if (dx < -12f) {
                        isRotatingLeft = true
                        isRotatingRight = false
                    } else {
                        isRotatingLeft = false
                        isRotatingRight = false
                        // Compensate and brake visual drift over target pad
                        if (velX > 0.12f) {
                            velX -= 0.08f
                        } else if (velX < -0.12f) {
                            velX += 0.08f
                        }
                    }

                    // Altitude velocity adjustments
                    val targetVelY = if (dy > 180f) 2.2f else if (dy > 60f) 1.0f else 0.45f
                    if (velY > targetVelY) {
                        isPushingThrust = true
                    } else {
                        isPushingThrust = false
                    }
                }

                // 2. Direct Translation & Self-Stabilizing Steering
                val lateralControlStrength = 0.16f + (thrusterLevel * 0.02f)
                var lateralAx = 0f
                
                if (isRotatingLeft && fuel > 0f) {
                    lateralAx = -lateralControlStrength
                    if (!isAutoLanderActive) {
                        fuel = maxOf(0f, fuel - burnRate * 0.25f)
                    }
                    // Visual minor kinetic tilt, self-corrects on release
                    angle = (angle - 2f).coerceAtLeast(-12f)
                } else if (isRotatingRight && fuel > 0f) {
                    lateralAx = lateralControlStrength
                    if (!isAutoLanderActive) {
                        fuel = maxOf(0f, fuel - burnRate * 0.25f)
                    }
                    angle = (angle + 2f).coerceIn(-12f, 12f)
                } else {
                    // Gradual angle home-stabilization
                    if (angle > 0f) {
                        angle = (angle - 1.5f).coerceAtLeast(0f)
                    } else if (angle < 0f) {
                        angle = (angle + 1.5f).coerceAtMost(0f)
                    }
                }

                // 3. Main Thrusters propulsion
                var ax = 0f
                var ay = 0f
                if (isPushingThrust && fuel > 0f) {
                    fuel = maxOf(0f, fuel - burnRate)
                    val angleRad = angle * PI / 180f
                    ax = (engineThrust * sin(angleRad)).toFloat()
                    ay = (-engineThrust * cos(angleRad)).toFloat()
                    
                    // Shake camera slightly on active thrusters
                    cameraShakeX = Random.nextDouble(-1.5, 1.5).toFloat()
                    cameraShakeY = Random.nextDouble(-1.5, 1.5).toFloat()

                    // Spawn engine exhausts
                    val angleRadRev = (angle + 180f) * PI / 180f
                    val exhaustX = posX + (14 * sin(angleRadRev)).toFloat()
                    val exhaustY = posY + (14 * cos(angleRadRev)).toFloat()
                    repeat(2) {
                        particles.add(
                            ThrusterParticle(
                                x = exhaustX,
                                y = exhaustY,
                                vx = (1.5 * sin(angleRadRev + Random.nextDouble(-0.3, 0.3))).toFloat() * 1.5f + velX * 0.5f,
                                vy = (1.5 * cos(angleRadRev + Random.nextDouble(-0.3, 0.3))).toFloat() * 1.5f + velY * 0.5f,
                                age = 0,
                                maxAge = Random.nextInt(20, 35),
                                color = if (Random.nextBoolean()) Color(0xFFFF7A00) else Color(0xFFFFD600),
                                size = Random.nextFloat() * 4f + 2f
                            )
                        )
                    }
                } else {
                    cameraShakeX = 0f
                    cameraShakeY = 0f
                }

                // 4. Environmental Physics integration
                val windDrift = planet.windPower
                velX += ax + lateralAx + windDrift
                velY += ay + planet.gravity

                // Atmosphere air friction damping
                velX *= dragDensity
                velY *= dragDensity

                // Update position
                posX += velX
                posY += velY

                // Left/Right walls bounce logic
                if (posX < 0f) {
                    posX = 0f
                    velX = -velX * 0.4f
                } else if (posX > canvasWidth) {
                    posX = canvasWidth
                    velX = -velX * 0.4f
                }
                
                // Top sky boundary threshold
                if (posY < 5f) {
                    posY = 5f
                    velY = maxOf(0f, velY)
                }

                // 4. Update smoke trails
                val particleIterator = particles.iterator()
                while (particleIterator.hasNext()) {
                    val p = particleIterator.next()
                    p.age++
                    p.x += p.vx
                    p.y += p.vy
                    if (p.age >= p.maxAge) {
                        particleIterator.remove()
                    }
                }

                // 5. Collision detection check
                terrainData?.let { data ->
                    val terrainYAtPosX = interpolateTerrainY(posX, data.points)
                    // Collision boundary triggers touchdown/impact (ship size clearance height matches landing gears at ~19f in scale)
                    val shipBottomY = posY + 19.0f
                    if (shipBottomY >= terrainYAtPosX) {
                        // Game Over: evaluate Landing
                        cameraShakeX = 0f
                        cameraShakeY = 0f
                        
                        val isOnPad = posX >= data.padX && posX <= (data.padX + data.padWidth)
                        val isSafeAngle = Math.abs(angle) <= 15f
                        val isSafeVerticalSpeed = velY <= safeSpeedLimit
                        val isSafeHorizontalSpeed = Math.abs(velX) <= safeSpeedLimit * 0.8f

                        if (isOnPad && isSafeAngle && isSafeVerticalSpeed && isSafeHorizontalSpeed) {
                            // Perfect touchdown
                            viewModel.processLanderLanding(fuel, velY)
                        } else {
                            // Crash landing!
                            viewModel.processLanderCrash(velY)
                        }
                    }
                }

                delay(16) // ~60 FPS update
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FLIGHT INSTRUMENT DECK", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = Color(0xFFD1E5F4), fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(SelectedScreen.Missions) },
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            planet.name.uppercase(),
                            color = planet.primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .border(1.dp, planet.primaryColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF111318))
            )
        },
        containerColor = Color(0xFF0A0A0B)
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            
            // Set canvas size for calculations
            LaunchedEffect(width, height) {
                canvasWidth = width
                canvasHeight = height
            }

            if (terrainData != null) {
                // Interactive space flight field
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("flight_sim_canvas")
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    // Make sure tapping background resets focus or can be auxiliary thrust
                                }
                            )
                        }
                ) {
                    val scaleX = size.width
                    val scaleY = size.height

                    // Draw beautiful parent gas giant planet with glowing rings in background celestial sky
                    val parentPlanetCenter = Offset(scaleX * 0.82f, scaleY * 0.18f)
                    drawCircle(
                        color = planet.primaryColor.copy(alpha = 0.12f),
                        radius = 48f,
                        center = parentPlanetCenter
                    )
                    drawCircle(
                        color = planet.primaryColor.copy(alpha = 0.22f),
                        radius = 48f,
                        center = parentPlanetCenter,
                        style = Stroke(width = 1.5f)
                    )
                    rotate(degrees = -15f, pivot = parentPlanetCenter) {
                        drawOval(
                            color = planet.primaryColor.copy(alpha = 0.08f),
                            topLeft = Offset(parentPlanetCenter.x - 75f, parentPlanetCenter.y - 12f),
                            size = Size(150f, 24f),
                            style = Stroke(width = 3.5f)
                        )
                    }

                    // Draw another sleek distant moon
                    drawCircle(
                        color = Color(0xFFE2E8F0).copy(alpha = 0.06f),
                        radius = 18f,
                        center = Offset(scaleX * 0.15f, scaleY * 0.28f)
                    )

                    // Twinkle stars rendering
                    stars.forEach { pos ->
                        drawCircle(
                            color = Color.White.copy(alpha = Random.nextFloat() * 0.4f + 0.4f),
                            radius = Random.nextFloat() * 1.5f + 1f,
                            center = pos
                        )
                    }

                    // Render cross wind current flows
                    if (planet.windPower != 0f) {
                        val flowOffset = (System.currentTimeMillis() % 4000).toFloat() / 4000f
                        val windColor = planet.primaryColor.copy(alpha = 0.08f)
                        val numLines = 4
                        for (j in 1..numLines) {
                            val lineY = scaleY * 0.15f * j
                            val lineXStart = (scaleX * flowOffset * (if (planet.windPower > 0) 1 else -1)) % scaleX
                            val finishedX = if (lineXStart < 0) lineXStart + scaleX else lineXStart
                            drawLine(
                                color = windColor,
                                start = Offset(finishedX, lineY),
                                end = Offset(finishedX + 80f, lineY + (planet.windPower * 100f)),
                                strokeWidth = 2f
                            )
                        }
                    }

                    // Draw Landing Target Guidance Guideline (Auxiliary support)
                    if (viewModel.gameState == GameState.InFlight) {
                        val verticalGlow = planet.primaryColor.copy(alpha = 0.18f)
                        // Dynamic guideline indicating lander offset alignment
                        drawLine(
                            color = verticalGlow,
                            start = Offset(posX, posY + 19.0f),
                            end = Offset(terrainData.padX + (terrainData.padWidth / 2f), terrainData.padY),
                            strokeWidth = 2f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 15f), 0f)
                        )
                    }

                    // 1. Draw magnificent background layered shadow mountains (depth projection)
                    val bgMountainPath = Path().apply {
                        moveTo(0f, scaleY)
                        terrainData.points.forEach { (tx, ty) ->
                            val bgX = (tx + 120f) % scaleX
                            val bgY = ty - 45f + (Math.sin(tx.toDouble() * 0.008) * 15).toFloat()
                            lineTo(bgX, bgY)
                        }
                        lineTo(scaleX, scaleY)
                        close()
                    }
                    drawPath(
                        path = bgMountainPath,
                        color = planet.secondaryColor.copy(alpha = 0.25f)
                    )

                    // 2. Draw procedural planet solid vector terrain with rich vertical gradient
                    val terrainPath = Path().apply {
                        moveTo(0f, scaleY)
                        terrainData.points.forEach { (tx, ty) ->
                            lineTo(tx, ty)
                        }
                        lineTo(scaleX, scaleY)
                        close()
                    }
                    drawPath(
                        path = terrainPath,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                planet.secondaryColor,
                                Color(0xFF0D0E11)
                            ),
                            startY = scaleY * 0.5f,
                            endY = scaleY
                        )
                    )

                    // 3. Draw procedural planet neon outline vector ridges with subtle glow
                    terrainData.points.zipWithNext { p1, p2 ->
                        drawLine(
                            color = planet.primaryColor.copy(alpha = 0.35f),
                            start = Offset(p1.first, p1.second - 1f),
                            end = Offset(p2.first, p2.second - 1f),
                            strokeWidth = 5f
                        )
                        drawLine(
                            color = planet.primaryColor,
                            start = Offset(p1.first, p1.second),
                            end = Offset(p2.first, p2.second),
                            strokeWidth = 2.5f
                        )
                    }

                    // 4. Draw Landing Pad Runway Concrete Steel Structure
                    val padX = terrainData.padX
                    val padWidth = terrainData.padWidth
                    val padY = terrainData.padY

                    // Concrete foundation block under the landing zone
                    drawRect(
                        color = Color(0xFF1E2022),
                        topLeft = Offset(padX, padY),
                        size = Size(padWidth, 14f)
                    )
                    drawRect(
                        color = Color(0xFF8A929A),
                        topLeft = Offset(padX, padY),
                        size = Size(padWidth, 14f),
                        style = Stroke(width = 1.5f)
                    )

                    // Draw yellow and black hazard warning stripes inside foundation block!
                    val stripeWidth = 6f
                    val numStripes = (padWidth / (stripeWidth * 2)).toInt()
                    for (s in 0..numStripes) {
                        val startStripeX = padX + (s * stripeWidth * 2)
                        val stripePath = Path().apply {
                            moveTo(startStripeX, padY + 14f)
                            lineTo(startStripeX + stripeWidth, padY)
                            lineTo(startStripeX + stripeWidth * 2f, padY)
                            lineTo(startStripeX + stripeWidth, padY + 14f)
                            close()
                        }
                        drawPath(
                            path = stripePath,
                            color = Color(0xFFFDE047).copy(alpha = 0.4f)
                        )
                    }
                    
                    // Main safety neon green runway deck top-rail
                    val padColor = Color(0xFF10B981)
                    drawLine(
                        color = padColor,
                        start = Offset(padX, padY),
                        end = Offset(padX + padWidth, padY),
                        strokeWidth = 3.5f
                    )

                    // Draw green alignment dots (beacons) on pad surface
                    val numBeacons = 5
                    for (b in 0 until numBeacons) {
                        val beaconX = padX + (padWidth / (numBeacons - 1)) * b
                        drawCircle(
                            color = if ((System.currentTimeMillis() % 1000) > 500) padColor else padColor.copy(alpha = 0.4f),
                            radius = 2.5f,
                            center = Offset(beaconX, padY + 1f)
                        )
                    }

                    // Draw metallic antenna tall beacon towers on both pad bounds
                    drawLine(
                        color = Color(0xFF8A929A),
                        start = Offset(padX, padY),
                        end = Offset(padX, padY - 14f),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color(0xFF8A929A),
                        start = Offset(padX + padWidth, padY),
                        end = Offset(padX + padWidth, padY - 14f),
                        strokeWidth = 2f
                    )

                    // Blinking strobe lights on top of antennas (flashing red and white)
                    val strobeTime = System.currentTimeMillis() % 600
                    val isStrobeLit = strobeTime < 300
                    val leftStrobeColor = if (isStrobeLit) Color(0xFFEF4444) else Color(0xFF7F1D1D)
                    val rightStrobeColor = if (isStrobeLit) Color.White else Color(0x33FFFFFF)

                    drawCircle(color = leftStrobeColor, radius = 4f, center = Offset(padX, padY - 14f))
                    drawCircle(color = rightStrobeColor, radius = 4f, center = Offset(padX + padWidth, padY - 14f))

                    // Draw exhaust thruster particles
                    particles.forEach { p ->
                        drawCircle(color = p.color.copy(alpha = 1f - (p.age.toFloat() / p.maxAge)), radius = p.size, center = Offset(p.x, p.y))
                    }

                    // Draw Apollo Spaceship vector with transform rotation support
                    val shipPivot = Offset(posX + cameraShakeX, posY + cameraShakeY)
                    rotate(degrees = angle, pivot = shipPivot) {
                        val x = shipPivot.x
                        val y = shipPivot.y

                        // Main rocket engine main flame / superheated plume
                        if (isPushingThrust && fuel > 0f) {
                            val flameHeight = Random.nextFloat() * 16f + 14f
                            val firePath = Path().apply {
                                moveTo(x - 5f, y + 11f)
                                lineTo(x, y + 11f + flameHeight)
                                lineTo(x + 5f, y + 11f)
                                close()
                            }
                            val innerFirePath = Path().apply {
                                moveTo(x - 2.5f, y + 11f)
                                lineTo(x, y + 11f + flameHeight * 0.6f)
                                lineTo(x + 2.5f, y + 11f)
                                close()
                            }
                            drawPath(
                                path = firePath,
                                color = Color(0xFFFF5252)
                            )
                            drawPath(
                                path = innerFirePath,
                                color = Color(0xFFFFFFE0)
                            )
                        }

                        // 1. Sleek metallic ship wings / stabilizers on the flanks
                        val wingPathLeft = Path().apply {
                            moveTo(x - 12f, y + 2f)
                            lineTo(x - 20f, y + 10f)
                            lineTo(x - 12f, y + 12f)
                            close()
                        }
                        val wingPathRight = Path().apply {
                            moveTo(x + 12f, y + 2f)
                            lineTo(x + 20f, y + 10f)
                            lineTo(x + 12f, y + 12f)
                            close()
                        }
                        drawPath(path = wingPathLeft, color = Color(0xFF475569))
                        drawPath(path = wingPathRight, color = Color(0xFF475569))

                        // 2. Main cylindrical-capsule high-tech Lander cabin hull
                        val cabinPath = Path().apply {
                            moveTo(x - 12f, y + 10f)
                            lineTo(x - 10f, y - 6f)
                            lineTo(x + 10f, y - 6f)
                            lineTo(x + 12f, y + 10f)
                            close()
                        }
                        drawPath(path = cabinPath, color = Color(0xFFECEFF1))

                        // Tech panel lines
                        drawRect(
                            color = Color(0xFFCFD8DC),
                            topLeft = Offset(x - 6f, y + 2f),
                            size = Size(12f, 8f)
                        )

                        // 3. Cockpit visor (sleek hexagonal dynamic dark-cyan glass visor)
                        val canopyPath = Path().apply {
                            moveTo(x - 6f, y - 1f)
                            lineTo(x - 4f, y - 5f)
                            lineTo(x + 4f, y - 5f)
                            lineTo(x + 6f, y - 1f)
                            lineTo(x, y + 1f)
                            close()
                        }
                        drawPath(
                            path = canopyPath,
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(Color(0xFF80DEEA), Color(0xFF00ACC1)),
                                center = Offset(x, y - 3f),
                                radius = 8f
                            )
                        )

                        // 4. Base high-temperature thruster bracket bracket
                        drawRect(
                            color = Color(0xFF263238),
                            topLeft = Offset(x - 5f, y + 10f),
                            size = Size(10f, 3f)
                        )

                        // 5. Advanced shock absorption hydraulic landing struts
                        val landingGold = Color(0xFFFFB300)
                        
                        drawLine(
                            color = Color(0xFF78909C),
                            start = Offset(x - 8f, y + 7f),
                            end = Offset(x - 17f, y + 19f),
                            strokeWidth = 2.5f
                        )
                        drawLine(
                            color = landingGold,
                            start = Offset(x - 3f, y + 9f),
                            end = Offset(x - 12f, y + 15f),
                            strokeWidth = 1.5f
                        )

                        drawLine(
                            color = Color(0xFF78909C),
                            start = Offset(x + 8f, y + 7f),
                            end = Offset(x + 17f, y + 19f),
                            strokeWidth = 2.5f
                        )
                        drawLine(
                            color = landingGold,
                            start = Offset(x + 3f, y + 9f),
                            end = Offset(x + 12f, y + 15f),
                            strokeWidth = 1.5f
                        )

                        // Contact footplates
                        drawCircle(
                            color = Color(0xFF37474F),
                            radius = 4f,
                            center = Offset(x - 17f, y + 19f)
                        )
                        drawCircle(
                            color = Color(0xFF37474F),
                            radius = 4f,
                            center = Offset(x + 17f, y + 19f)
                        )

                        // 6. Navigation light flashing beacon at the top of the cabin
                        val timePulse = System.currentTimeMillis() % 1000
                        val topBeaconColor = if (timePulse > 500) Color(0xFFFFE082) else Color(0xFFFF8F00)
                        drawCircle(
                            color = topBeaconColor,
                            radius = 2.0f,
                            center = Offset(x, y - 7f)
                        )

                        // 7. Micro-reaction RCS side attitude thrusters firing
                        if (isRotatingRight) {
                            drawLine(
                                color = Color(0xFF81D4FA),
                                start = Offset(x - 14f, y - 2f),
                                end = Offset(x - 22f, y - 2f),
                                strokeWidth = 2f
                            )
                        }
                        if (isRotatingLeft) {
                            drawLine(
                                color = Color(0xFF81D4FA),
                                start = Offset(x + 14f, y - 2f),
                                end = Offset(x + 22f, y - 2f),
                                strokeWidth = 2f
                            )
                        }
                    }
                }

                // Top UI Dashboard Dashboard readout overlay
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .background(Color(0xFF111318).copy(alpha = 0.88f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF43474E), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Fuel capacity status gauge
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocalGasStation, contentDescription = "Fuel", tint = Color(0xFFFFB000), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "FUEL RESERVE",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "${String.format("%.1f", fuel)} / ${maxFuel.toInt()} L",
                                color = if (fuel < maxFuel * 0.25f) Color(0xFFEF4444) else Color(0xFFF1F5F9),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("flight_fuel_indicator")
                            )
                        }

                        // Ascent/Descent Speed limit stats readout
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Speed, contentDescription = "Speed", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "DESCENT RATE",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            val isSafe = velY <= safeSpeedLimit
                            Text(
                                "${String.format("%.2f", velY)} m/s",
                                color = if (isSafe) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("flight_speed_indicator")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Fuel linear warning bar
                    LinearProgressIndicator(
                        progress = { fuel / maxFuel },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (fuel < maxFuel * 0.25f) Color(0xFFEF4444) else planet.primaryColor,
                        trackColor = Color(0xFF1E293B)
                    )

                    // Diagnostic alerts / alignment helpers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val dx = (terrainData.padX + (terrainData.padWidth / 2f)) - posX
                        val directionText = if (isAutoLanderActive) "⚡ AUTO-LANDING..." else if (dx > 20f) "STEER EAST ➜" else if (dx < -20f) "↞ STEER WEST" else "ALIGN TO PAD ✓"
                        val alignmentColor = if (isAutoLanderActive) Color(0xFF10B981) else if (Math.abs(dx) <= terrainData.padWidth / 2f) Color(0xFF10B981) else Color(0xFFFBBF24)

                        Text(
                            directionText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = alignmentColor
                        )

                        Text(
                            "CROSSWIND: ${if (planet.windPower > 0) "E" else "W"} ${String.format("%.1f", Math.abs(planet.windPower) * 200)} kn",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (planet.windPower == 0f) Color(0xFF475569) else Color(0xFF94A3B8)
                        )

                        Text(
                            "TILT: ${String.format("%.0f", angle)}°",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (Math.abs(angle) <= 15f) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }

                // Non-Inflight Launch panel overlays
                if (viewModel.gameState == GameState.Launching) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xB3020617)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .padding(16.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .background(Color(0xFF0F172A))
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "DESCENT MODULE READY",
                                    color = planet.primaryColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Planet: ${planet.name}\n" +
                                            "Gravity: ${String.format("%.1f", planet.gravity * 36)} m/s²\n" +
                                            "Atmospheric Wind: ${if (planet.windPower > 0) "Eastward" else if (planet.windPower < 0) "Westward" else "Calm"}\n" +
                                            "Max Impact Velocity Allowance: ${String.format("%.2f", safeSpeedLimit)} m/s",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.startSimulation() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("launch_simulation_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = planet.primaryColor)
                                ) {
                                    Text("INITIATE ROCKET BREAKOUT", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Flight controller HUD panels at bottom
                if (viewModel.gameState == GameState.InFlight) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 24.dp)
                                .windowInsetsPadding(WindowInsets.navigationBars),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Sleek design for Auto-Lander override toggle
                            Button(
                                onClick = { isAutoLanderActive = !isAutoLanderActive },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAutoLanderActive) Color(0xCC10B981) else Color(0xCC1E293B),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(100.dp),
                                border = BorderStroke(1.5.dp, if (isAutoLanderActive) Color(0xFF34D399) else Color(0xFF43474E)),
                                modifier = Modifier
                                    .testTag("auto_lander_toggle_control")
                                    .height(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAutoLanderActive) Icons.Filled.BrightnessAuto else Icons.Filled.Toys,
                                    contentDescription = "Autopilot Override",
                                    tint = if (isAutoLanderActive) Color.White else Color(0xFFD1E5F4),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAutoLanderActive) "AUTOPILOT ENGAGED (AP-90)" else "ENGAGE AUTO-LANDER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // STEERING lateral thrusters on left (Combined touch zones with manual override)
                                Row(
                                    modifier = Modifier.wrapContentSize(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Translate Left
                                    Box(
                                        modifier = Modifier
                                            .size(62.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x991E293B))
                                            .border(1.5.dp, Color(0xFF43474E), CircleShape)
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onPress = {
                                                        isAutoLanderActive = false // Manual override!
                                                        isRotatingLeft = true
                                                        tryAwaitRelease()
                                                        isRotatingLeft = false
                                                    }
                                                )
                                            }
                                            .testTag("rotate_left_control"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.ChevronLeft,
                                            contentDescription = "Translate Left",
                                            tint = Color.White,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }

                                    // Translate Right
                                    Box(
                                        modifier = Modifier
                                            .size(62.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x991E293B))
                                            .border(1.5.dp, Color(0xFF43474E), CircleShape)
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onPress = {
                                                        isAutoLanderActive = false // Manual override!
                                                        isRotatingRight = true
                                                        tryAwaitRelease()
                                                        isRotatingRight = false
                                                    }
                                                )
                                            }
                                            .testTag("rotate_right_control"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.ChevronRight,
                                            contentDescription = "Translate Right",
                                            tint = Color.White,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                // Massive dynamic MAIN PROPULSION ignition pedal on right
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(if (isPushingThrust) Color(0xCCEF4444) else Color(0x991E293B))
                                        .border(2.dp, if (isPushingThrust) Color(0xFFFF7A00) else Color(0xFFEF4444), CircleShape)
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    isAutoLanderActive = false // Manual override!
                                                    isPushingThrust = true
                                                    tryAwaitRelease()
                                                    isPushingThrust = false
                                                }
                                            )
                                        }
                                        .testTag("propulsion_thrust_control"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Filled.LocalFireDepartment,
                                            contentDescription = "Booster thrust",
                                            tint = if (isPushingThrust) Color.Yellow else Color(0xFFEF4444),
                                            modifier = Modifier.size(38.dp)
                                        )
                                        Text(
                                            "THRUST",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Crash & Landing Success Result Sheets
                val currentState = viewModel.gameState
                AnimatedVisibility(
                    visible = currentState is GameState.Success || currentState is GameState.Crashed,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xF0020617))
                            .clickable(enabled = false) {},
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Column(
                                modifier = Modifier
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                when (currentState) {
                                    is GameState.Success -> {
                                        Icon(
                                            Icons.Filled.WorkspacePremium,
                                            contentDescription = "Mission Success",
                                            tint = Color(0xFFFFD600),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "MISSION COMPLETED",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF10B981)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            currentState.statsText,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFCBD5E1),
                                            lineHeight = 18.sp,
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Total summary points accrued
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceAround
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("CREDITS", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                                Text("+${currentState.earnedCredits} Cr", fontSize = 18.sp, color = Color(0xFFFFD600), fontWeight = FontWeight.Bold)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("PILOT XP", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                                Text("+${currentState.earnedXp} XP", fontSize = 18.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Milestone & Level ups alert overlays
                                        if (currentState.alerts.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                currentState.alerts.forEach { alert ->
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Filled.NotificationsActive, "Alert", tint = Color(0xFFFFB000), modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(alert, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    is GameState.Crashed -> {
                                        Icon(
                                            Icons.Filled.Warning,
                                            contentDescription = "Failure Crash",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "SHIP DISINTEGRATED",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFEF4444)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            currentState.statsText,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFCBD5E1),
                                            textAlign = TextAlign.Center
                                        )

                                        if (currentState.alerts.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                currentState.alerts.forEach { alert ->
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Filled.NotificationsActive, "Alert", tint = Color(0xFFFFB000), modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(alert, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    else -> {}
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.navigateTo(SelectedScreen.Missions) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("ORBITAL SELECTION", fontSize = 12.sp, color = Color.White)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.navigateTo(SelectedScreen.FlightSimulator)
                                        },
                                        modifier = Modifier.weight(1f)
                                            .testTag("retry_run_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = planet.primaryColor)
                                    ) {
                                        Text("RELAUNCH POD", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Simple Linear Line Interpolator to grab exact Surface height relative to spacecraft coordinates
private fun interpolateTerrainY(posX: Float, points: List<Pair<Float, Float>>): Float {
    if (points.isEmpty()) return 1000f
    
    // Check boundaries
    if (posX <= points.first().first) return points.first().second
    if (posX >= points.last().first) return points.last().second

    for (i in 0 until points.size - 1) {
        val p1 = points[i]
        val p2 = points[i + 1]
        if (posX >= p1.first && posX <= p2.first) {
            val deltaX = p2.first - p1.first
            if (deltaX == 0f) return p1.second
            val progress = (posX - p1.first) / deltaX
            return p1.second + progress * (p2.second - p1.second)
        }
    }
    return points.last().second
}
