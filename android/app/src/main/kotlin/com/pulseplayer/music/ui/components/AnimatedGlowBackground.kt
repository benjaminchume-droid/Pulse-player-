package com.pulseplayer.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.pulseplayer.music.ui.theme.RealmManager
import com.pulseplayer.music.ui.theme.RealmTheme
import com.pulseplayer.music.ui.theme.PerformanceMode
import kotlin.random.Random

// Inline structural representation of floating dust particles
data class FloatingParticle(
    var x: Float,
    var y: Float,
    val r: Float,
    val speedX: Float,
    val speedY: Float,
    val alpha: Float
) {
    fun update(height: Float, width: Float, tempoBoost: Float) {
        x += speedX * (1f + tempoBoost * 2f)
        y += speedY * (1f + tempoBoost * 2f)
        if (x < 0) x = width
        if (x > width) x = 0f
        if (y < 0) y = height
        if (y > height) y = 0f
    }
}

@Composable
fun AnimatedGlowBackground(modifier: Modifier = Modifier) {
    val theme by RealmManager.currentTheme.collectAsState()
    val perfMode by RealmManager.performanceMode.collectAsState()
    val isAmoled by RealmManager.amoledMode.collectAsState()

    val bass by RealmManager.bassIntensity.collectAsState()
    val mid by RealmManager.midIntensity.collectAsState()
    val treble by RealmManager.trebleIntensity.collectAsState()

    // Smooth color state transitions when swapping theme realms
    val animatedBgColor1 by animateColorAsState(
        targetValue = if (isAmoled) Color.Black else theme.gradientColors.getOrElse(0) { Color.Black },
        animationSpec = tween(1500),
        label = "BgColor1"
    )
    val animatedBgColor2 by animateColorAsState(
        targetValue = if (isAmoled) Color.Black else theme.gradientColors.getOrElse(1) { Color.Black },
        animationSpec = tween(1500),
        label = "BgColor2"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "AtmosphereOrbTransition")

    // Perpetual kinetic drifts for gaseous light spheres
    val driftX1 by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 500f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DriftX1"
    )
    val driftY1 by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = 850f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DriftY1"
    )
    val driftX2 by infiniteTransition.animateFloat(
        initialValue = 800f,
        targetValue = 150f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DriftX2"
    )
    val driftY2 by infiniteTransition.animateFloat(
        initialValue = 200f,
        targetValue = 950f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DriftY2"
    )

    // Particle state setup
    val particles = remember(theme.id, perfMode) {
        val count = if (perfMode == PerformanceMode.LITE) 0 
                    else if (perfMode == PerformanceMode.BALANCED) theme.particleCount / 2 
                    else theme.particleCount
        
        List(count) {
            FloatingParticle(
                x = Random.nextFloat() * 1200f,
                y = Random.nextFloat() * 2000f,
                r = Random.nextFloat() * 5f + 2f,
                speedX = (Random.nextFloat() * 1.6f - 0.8f),
                speedY = (Random.nextFloat() * 1.6f - 0.8f) - 0.5f, // naturally drift upwards
                alpha = Random.nextFloat() * 0.5f + 0.2f
            )
        }
    }

    // Keep state ticking for the rendering clock
    val tick = remember { mutableStateOf(0L) }
    LaunchedEffect(perfMode) {
        if (perfMode != PerformanceMode.LITE) {
            while (true) {
                withFrameMillis { frameTime ->
                    tick.value = frameTime
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(animatedBgColor1, animatedBgColor2)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw glowing background soundstage spheres (Atmosphere)
            if (!isAmoled || perfMode == PerformanceMode.CINEMATIC) {
                // Adaptive reactive glow scaling based on Bass beats
                val glow1Scale = 1f + (bass * 0.45f)
                val glow2Scale = 1f + (mid * 0.3f)

                val radius1 = 400f * glow1Scale
                val radius2 = 360f * glow2Scale

                // Left top glowing atmospheric orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            theme.accentColor.copy(alpha = if (isAmoled) 0.08f else 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(driftX1, driftY1),
                        radius = radius1
                    ),
                    radius = radius1,
                    center = Offset(driftX1, driftY1)
                )

                // Right bottom glowing atmospheric orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            theme.glowColor.copy(alpha = if (isAmoled) 0.06f else 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(driftX2, driftY2),
                        radius = radius2
                    ),
                    radius = radius2,
                    center = Offset(driftX2, driftY2)
                )
            }

            // 2. Cyberpunk Scanline Grid & Horizon drawing for retro theme properties
            if (theme.visualizerStyle == "retro" && perfMode != PerformanceMode.LITE) {
                val gridOpacity = 0.06f + (bass * 0.04f)
                val lineSpacing = 40f
                val hOffset = (tick.value / 12) % lineSpacing.toLong()
                
                // Draw ground-grid perspective lines
                val horizonY = height * 0.65f
                for (x in 0..width.toInt() step 80) {
                    drawLine(
                        color = theme.glowColor.copy(alpha = gridOpacity),
                        start = Offset(x.toFloat(), height),
                        end = Offset(width / 2f + (x - width / 2f) * 0.15f, horizonY),
                        strokeWidth = 1.5f
                    )
                }
                // Horizontal lines sliding down
                var y = horizonY
                var spacingMultiplier = 1.0f
                while (y < height) {
                    val lineY = y + hOffset.toFloat() * spacingMultiplier
                    if (lineY in horizonY..height) {
                        drawLine(
                            color = theme.accentColor.copy(alpha = gridOpacity),
                            start = Offset(0f, lineY),
                            end = Offset(width, lineY),
                            strokeWidth = 1f
                        )
                    }
                    y += lineSpacing * spacingMultiplier
                    spacingMultiplier *= 1.15f // perspective stretch
                }
            }

            // 3. Radioactive Industrial Warning Hazard Grating
            if (theme.visualizerStyle == "industrial" && !isAmoled) {
                // Subtle glowing warning chevrons
                val stripesOpacity = 0.03f + (mid * 0.02f)
                val stripeWidth = 60f
                for (offset in -400..width.toInt() step (stripeWidth * 2f).toInt()) {
                    drawLine(
                        color = theme.accentColor.copy(alpha = stripesOpacity),
                        start = Offset(offset.toFloat(), 0f),
                        end = Offset(offset.toFloat() + 300f, height),
                        strokeWidth = stripeWidth
                    )
                }
            }

            // 4. Update and Draw Particle System
            if (perfMode != PerformanceMode.LITE && particles.isNotEmpty()) {
                // Ensure tick.value reference keeps this executing
                tick.value 
                
                particles.forEach { p ->
                    // Velocity responds to the Mid/Treble volume levels for visual dancing
                    p.update(height, width, mid)
                    
                    val reactiveRadius = p.r * (1f + treble * 0.6f)
                    val particleAlpha = p.alpha * (0.3f + (treble * 0.7f))

                    drawCircle(
                        color = theme.particleColor.copy(alpha = particleAlpha),
                        radius = reactiveRadius,
                        center = Offset(p.x, p.y)
                    )
                }
            }
        }
    }
}
