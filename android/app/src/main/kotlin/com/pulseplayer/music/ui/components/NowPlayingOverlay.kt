package com.pulseplayer.music.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulseplayer.music.data.Song
import com.pulseplayer.music.ui.theme.RealmManager
import com.pulseplayer.music.ui.theme.RealmTheme
import com.pulseplayer.music.ui.theme.PerformanceMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NowPlayingOverlay(
    isOpen: Boolean,
    song: Song?,
    isPlaying: Boolean,
    playbackPosition: Long,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleFavorite: () -> Unit
) {
    val theme by RealmManager.currentTheme.collectAsState()
    val perfMode by RealmManager.performanceMode.collectAsState()
    val isAmoled by RealmManager.amoledMode.collectAsState()

    // Interactive custom state properties
    var showDspPanel by remember { mutableStateOf(false) }
    var activeDspStage by remember { mutableStateOf("Stereo Pro") }
    var spatialSpacialSoundstage by remember { mutableStateOf(0.5f) }
    var echoDecayTimes by remember { mutableStateOf(0.3f) }
    var manualBassBoost by remember { mutableStateOf(0.6f) }
    var dynamicWeatherMode by remember { mutableStateOf("Midnight Clear") }

    // Frequency state values for visualizer canvas
    val bass by RealmManager.bassIntensity.collectAsState()
    val mid by RealmManager.midIntensity.collectAsState()
    val treble by RealmManager.trebleIntensity.collectAsState()

    // 60FPS Coroutine simulation for natural audio waves in the visualizer when song is playing
    LaunchedEffect(isPlaying, manualBassBoost) {
        if (isPlaying) {
            var tick = 0f
            while (true) {
                // Waveform frequencies oscillating
                val calculatedBass = (sin(tick) * 0.4f + 0.6f) * manualBassBoost
                val calculatedMid = (cos(tick * 1.5f) * 0.35f + 0.45f) * (manualBassBoost * 0.9f)
                val calculatedTreble = (sin(tick * 2.3f) * 0.3f + 0.5f) * 0.8f

                RealmManager.bassIntensity.value = calculatedBass.coerceIn(0f, 1.2f)
                RealmManager.midIntensity.value = calculatedMid.coerceIn(0f, 1.2f)
                RealmManager.trebleIntensity.value = calculatedTreble.coerceIn(0f, 1.2f)

                tick += 0.08f
                delay(16) // tick every 16ms (~60 FPS)
            }
        } else {
            RealmManager.bassIntensity.value = 0.05f
            RealmManager.midIntensity.value = 0.05f
            RealmManager.trebleIntensity.value = 0.05f
        }
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        if (song == null) return@AnimatedVisibility

        val duration = song.duration
        val displayPos = if (playbackPosition > duration) duration else playbackPosition

        val backgroundModifier = if (isAmoled) {
            Modifier.background(Color.Black)
        } else {
            Modifier.background(Brush.verticalGradient(colors = theme.gradientColors))
        }

        // Outer layout wrapping
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(backgroundModifier)
        ) {
            // Background ambient dynamic fog layer
            if (!isAmoled && perfMode != PerformanceMode.LITE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    theme.glowColor.copy(alpha = 0.25f + bass * 0.15f),
                                    Color.Transparent
                                ),
                                radius = 600f
                            )
                        )
                )
            }

            // Scroll container enabling smooth navigation on compact devices
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Navigation header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimize Stage",
                            tint = if (theme.isLight) Color.Black else Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = theme.soundstageName.uppercase(),
                            color = theme.accentColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Sound Stage Connected",
                            color = if (theme.isLight) Color.DarkGray else Color.LightGray,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite State Toggle",
                            tint = if (song.isFavorite) theme.accentColor else (if (theme.isLight) Color.Black else Color.White),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 2. Custom Immersive Multi-layer Visualizer & Cover Artwork Frame with Gestures
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(if (isAmoled) Color(0xFF0C0C0C) else Color(0x19FFFFFF))
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(theme.accentColor, theme.glowColor.copy(alpha = 0.3f))
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                        // Gestures: Double tap to favorite, drag vertically to close
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { onToggleFavorite() },
                                onTap = { showDspPanel = !showDspPanel }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                if (dragAmount.y > 60f) {
                                    onClose()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Inside Canvas rendering stackable multi-layer dynamic visualizer
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val cx = width / 2f
                        val cy = height / 2f

                        // Visualizer layer: Waveform orbit rings
                        if (perfMode != PerformanceMode.LITE) {
                            val orbitRadius = 100f + (bass * 30f)
                            drawCircle(
                                color = theme.glowColor.copy(alpha = 0.12f),
                                radius = orbitRadius + 15f,
                                style = Stroke(width = 2f)
                            )

                            // Render specific realm wave geometries
                            when (theme.visualizerStyle) {
                                "galaxy" -> {
                                    // Circular spiral galaxy streams
                                    val pointsCount = 40
                                    for (i in 0 until pointsCount) {
                                        val angle = (i * (360f / pointsCount)) * (Math.PI / 180f)
                                        val length = orbitRadius + (sin(i * 1.5f + (bass * 5f)) * 12f)
                                        val endX = cx + (cos(angle) * length).toFloat()
                                        val endY = cy + (sin(angle) * length).toFloat()

                                        drawCircle(
                                            color = theme.accentColor.copy(alpha = 0.7f),
                                            radius = 3f + (treble * 2f),
                                            center = Offset(endX, endY)
                                        )
                                    }
                                }
                                "retro" -> {
                                    // Synthwave horizon grid lines
                                    drawLine(
                                        color = theme.accentColor,
                                        start = Offset(0f, cy + 20f),
                                        end = Offset(width, cy + 20f),
                                        strokeWidth = 2f
                                    )
                                    val peaks = 10
                                    val step = width / peaks
                                    for (i in 0..peaks) {
                                        val lineX = i * step
                                        val peakHeight = cy + 20f - (cos(i * 0.8f + (bass * 3f)) * (10f + treble * 25f))
                                        drawLine(
                                            color = theme.glowColor,
                                            start = Offset(lineX, cy + 20f),
                                            end = Offset(lineX, peakHeight),
                                            strokeWidth = 3f,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                                else -> {
                                    // Default dynamic frequency wave rings
                                    val segments = 60
                                    for (i in 0 until segments) {
                                        val angle = (i * (360f / segments)) * (Math.PI / 180f)
                                        val heightMod = (sin(i * 0.5f + (mid * 6f)) * (15f + treble * 30f))
                                        val startX = cx + (cos(angle) * orbitRadius).toFloat()
                                        val startY = cy + (sin(angle) * orbitRadius).toFloat()
                                        val endX = cx + (cos(angle) * (orbitRadius + heightMod)).toFloat()
                                        val endY = cy + (sin(angle) * (orbitRadius + heightMod)).toFloat()

                                        drawLine(
                                            color = theme.accentColor.copy(alpha = 0.85f),
                                            start = Offset(startX, startY),
                                            end = Offset(endX, endY),
                                            strokeWidth = 3f,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Album Center Crest
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(theme.accentColor.copy(alpha = 0.15f))
                                .border(1.dp, theme.accentColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = theme.glyphEmblems,
                                fontSize = 34.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "REALM PRESETS",
                            color = if (theme.isLight) Color.Black else Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Tap to toggle DSP config",
                            color = if (theme.isLight) Color.DarkGray else Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                }

                // 3. Ambient atmospheric weather indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0AFFFFFF))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🛰️", fontSize = 11.sp)
                        Text(
                            text = "REALM FREQUENCY MODE: $dynamicWeatherMode",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.accentColor,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // 4. Audio Metadata Card
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = song.title,
                        color = if (theme.isLight) Color.Black else Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist.uppercase(),
                        color = theme.accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.5.sp
                    )
                }

                // 5. DSP Panel Config Drawer (Toggleable)
                AnimatedVisibility(
                    visible = showDspPanel,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "DSP AUDIO SPACE CONTROL",
                                color = theme.accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )

                            // Spatial stage selector buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val stages = listOf("Stereo Pro", "Binaural Stage", "Abyssal Space")
                                stages.forEach { stage ->
                                    val isSel = activeDspStage == stage
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) theme.accentColor else Color(0x12FFFFFF))
                                            .clickable { 
                                                activeDspStage = stage 
                                                if (stage == "Abyssal Space") {
                                                    spatialSpacialSoundstage = 0.9f
                                                    echoDecayTimes = 0.8f
                                                    dynamicWeatherMode = "Abyssal Cave Echo"
                                                } else if (stage == "Binaural Stage") {
                                                    spatialSpacialSoundstage = 0.75f
                                                    echoDecayTimes = 0.4f
                                                    dynamicWeatherMode = "3D Cinematic Halo"
                                                } else {
                                                    spatialSpacialSoundstage = 0.3f
                                                    echoDecayTimes = 0.15f
                                                    dynamicWeatherMode = "Sunset Stereo"
                                                }
                                            }
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stage,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) (if (theme.isLight) Color.White else Color.Black) else Color.White
                                        )
                                    }
                                }
                            }

                            // Interactive Spatial & Frequency depth sliders
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Binaural Room Depth", color = Color.Gray, fontSize = 11.sp)
                                    Text("${(spatialSpacialSoundstage * 100).toInt()}%", color = Color.White, fontSize = 11.sp)
                                }
                                Slider(
                                    value = spatialSpacialSoundstage,
                                    onValueChange = { spatialSpacialSoundstage = it },
                                    colors = SliderDefaults.colors(activeTrackColor = theme.accentColor)
                                )
                            }

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Analog Resonance Decay", color = Color.Gray, fontSize = 11.sp)
                                    Text("${(echoDecayTimes * 10).toInt()}ms", color = Color.White, fontSize = 11.sp)
                                }
                                Slider(
                                    value = echoDecayTimes,
                                    onValueChange = { echoDecayTimes = it },
                                    colors = SliderDefaults.colors(activeTrackColor = theme.accentColor)
                                )
                            }

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Pre-Amp Bass Multiplier", color = Color.Gray, fontSize = 11.sp)
                                    Text("${String.format("%.1f", manualBassBoost * 2f)}x", color = Color.White, fontSize = 11.sp)
                                }
                                Slider(
                                    value = manualBassBoost,
                                    onValueChange = { manualBassBoost = it },
                                    colors = SliderDefaults.colors(activeTrackColor = theme.accentColor)
                                )
                            }
                        }
                    }
                }

                // 6. Custom Seek slider bar matching active theme colors
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = displayPos.toFloat(),
                        onValueChange = { onSeekTo(it.toLong()) },
                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            activeTrackColor = theme.accentColor,
                            inactiveTrackColor = if (theme.isLight) Color(0x1F000000) else Color(0x1AFFFFFF),
                            thumbColor = theme.accentColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatMillis(displayPos),
                            color = if (theme.isLight) Color.DarkGray else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatMillis(duration),
                            color = if (theme.isLight) Color.DarkGray else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 7. Master Controls Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onSkipPrevious) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Skip Previous",
                            tint = if (theme.isLight) Color.Black else Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Rounded Play/Pause Core Block
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(theme.accentColor)
                            .clickable(onClick = onTogglePlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play Control Toggle",
                            tint = if (theme.isLight) Color.White else Color.Black,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    IconButton(onClick = onSkipNext) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Skip Next",
                            tint = if (theme.isLight) Color.Black else Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

private fun formatMillis(millis: Long): String {
    val totalSecs = millis / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    return String.format("%d:%02d", minutes, seconds)
}
