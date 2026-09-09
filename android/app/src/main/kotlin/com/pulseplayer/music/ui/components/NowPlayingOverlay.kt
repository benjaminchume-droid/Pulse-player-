package com.pulseplayer.music.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pulseplayer.music.data.RepeatMode
import com.pulseplayer.music.data.Song
import com.pulseplayer.music.ui.theme.RealmManager

@Composable
fun NowPlayingOverlay(
    isOpen: Boolean,
    song: Song?,
    isPlaying: Boolean,
    playbackPosition: Long,
    lyricsLoading: Boolean = false,
    repeatMode: RepeatMode = RepeatMode.OFF,
    fullScreenLyrics: Boolean = false,
    onFullScreenLyrics: (Boolean) -> Unit = {},
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onFetchLyrics: () -> Unit = {},
    onEnrichMetadata: () -> Unit = {},
    onCycleRepeat: () -> Unit = {},
    onQueueNext: () -> Unit = {},
    onSleepTimer: (Int) -> Unit = {}
) {
    val theme by RealmManager.currentTheme.collectAsState()
    val isAmoled by RealmManager.amoledMode.collectAsState()
    val context = LocalContext.current
    var showSleepMenu by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        if (song == null) return@AnimatedVisibility

        val duration = song.duration.coerceAtLeast(1L)
        val displayPos = playbackPosition.coerceIn(0L, duration)
        val progress = (displayPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        val cover = song.coverUrl.ifBlank { null }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isAmoled) Color.Black else Color(0xFF0A0A12))
                .pointerInput(Unit) {
                    detectDragGestures { _, drag -> if (drag.y > 80f) onClose() }
                }
                .pointerInput(song.id) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount < -80f) onSkipNext()
                        else if (dragAmount > 80f) onSkipPrevious()
                    }
                }
        ) {
            // Blurred full-bleed cover background
            if (cover != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(cover).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(32.dp)
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
            } else {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(theme.gradientColors)
                    )
                )
            }

            if (fullScreenLyrics) {
                Column(Modifier.fillMaxSize().padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        IconButton(onClick = { onFullScreenLyrics(false) }) {
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White)
                        }
                        Text("Lyrics", color = theme.accentColor, fontWeight = FontWeight.Bold)
                        Spacer(modifier.size(48.dp))
                    }
                    LyricsPanel(
                        song = song,
                        positionMs = displayPos,
                        isLoading = lyricsLoading,
                        onFetchLyrics = onFetchLyrics,
                        modifier = Modifier.fillMaxSize().padding(top = 8.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.KeyboardArrowDown, "Close", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Text("NOW PLAYING", color = theme.accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                null,
                                tint = if (song.isFavorite) theme.accentColor else Color.White
                            )
                        }
                    }

                    // Cover art fills the artwork box
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(2.dp, theme.accentColor.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cover != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(cover).crossfade(true).build(),
                                contentDescription = song.album,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                Modifier.fillMaxSize().background(Color(0x22FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(theme.glyphEmblems, fontSize = 64.sp)
                            }
                        }
                    }

                    Text(song.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Light,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    Text(song.artist, color = theme.accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    if (song.album.isNotBlank() && song.album != "Unknown Album") {
                        Text(song.album, color = Color.Gray, fontSize = 12.sp)
                    }

                    Slider(
                        value = progress,
                        onValueChange = { onSeekTo((it * duration).toLong()) },
                        colors = SliderDefaults.colors(
                            thumbColor = theme.accentColor,
                            activeTrackColor = theme.accentColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatTime(displayPos), color = Color.Gray, fontSize = 11.sp)
                        Text(formatTime(duration), color = Color.Gray, fontSize = 11.sp)
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onCycleRepeat) {
                            Icon(
                                when (repeatMode) {
                                    RepeatMode.ONE -> Icons.Default.RepeatOne
                                    RepeatMode.ALL -> Icons.Default.Repeat
                                    RepeatMode.OFF -> Icons.Default.Repeat
                                },
                                "Repeat",
                                tint = if (repeatMode != RepeatMode.OFF) theme.accentColor else Color.White.copy(0.5f)
                            )
                        }
                        IconButton(onClick = onSkipPrevious) {
                            Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                        Box(
                            Modifier.size(64.dp).clip(CircleShape).background(theme.accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = onTogglePlayPause) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    null, tint = Color.Black, modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        IconButton(onClick = onSkipNext) {
                            Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                        Box {
                            IconButton(onClick = { showSleepMenu = true }) {
                                Icon(Icons.Default.Timer, "Sleep", tint = Color.White.copy(0.7f))
                            }
                            DropdownMenu(expanded = showSleepMenu, onDismissRequest = { showSleepMenu = false }) {
                                listOf(0, 5, 10, 15, 30, 45, 60).forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(if (m == 0) "Off" else "$m min") },
                                        onClick = {
                                            onSleepTimer(m)
                                            showSleepMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { onFullScreenLyrics(true) }) {
                            Text("Full lyrics", color = theme.accentColor)
                        }
                        TextButton(onClick = onEnrichMetadata) {
                            Text("Metadata", color = theme.accentColor)
                        }
                    }

                    LyricsPanel(
                        song = song,
                        positionMs = displayPos,
                        isLoading = lyricsLoading,
                        onFetchLyrics = onFetchLyrics,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
