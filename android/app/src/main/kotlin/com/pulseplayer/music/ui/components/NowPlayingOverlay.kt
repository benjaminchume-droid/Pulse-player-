package com.pulseplayer.music.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulseplayer.music.data.Song
import com.pulseplayer.music.ui.theme.RealmManager

@Composable
fun NowPlayingOverlay(
    isOpen: Boolean,
    song: Song?,
    isPlaying: Boolean,
    playbackPosition: Long,
    lyricsLoading: Boolean = false,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onFetchLyrics: () -> Unit = {},
    onEnrichMetadata: () -> Unit = {}
) {
    val theme by RealmManager.currentTheme.collectAsState()
    val isAmoled by RealmManager.amoledMode.collectAsState()

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        if (song == null) return@AnimatedVisibility

        val duration = song.duration.coerceAtLeast(1L)
        val displayPos = playbackPosition.coerceIn(0L, duration)
        val progress = (displayPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

        val bg = if (isAmoled) {
            Modifier.background(Color.Black)
        } else {
            Modifier.background(Brush.verticalGradient(theme.gradientColors))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(bg)
                .pointerInput(Unit) {
                    detectDragGestures { _, drag ->
                        if (drag.y > 80f) onClose()
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.KeyboardArrowDown, "Close", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NOW PLAYING", color = theme.accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("Pulse Player", color = Color.Gray, fontSize = 11.sp)
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Favorite",
                            tint = if (song.isFavorite) theme.accentColor else Color.White
                        )
                    }
                }

                // Artwork
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0x22FFFFFF))
                        .border(2.dp, theme.accentColor.copy(alpha = 0.5f), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(theme.glyphEmblems, fontSize = 64.sp)
                }

                // Title / artist / album metadata
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        song.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Light,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(song.artist, color = theme.accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    if (song.album.isNotBlank() && song.album != "Unknown Album") {
                        Text(song.album, color = Color.Gray, fontSize = 12.sp)
                    }
                    if (song.year > 0 || song.genre.isNotBlank()) {
                        Text(
                            listOfNotNull(
                                song.year.takeIf { it > 0 }?.toString(),
                                song.genre.takeIf { it.isNotBlank() && it != "All Streams" }
                            ).joinToString(" · "),
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    TextButton(onClick = onEnrichMetadata) {
                        Text("Refresh metadata", color = theme.accentColor, fontSize = 11.sp)
                    }
                }

                // Seek bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = progress,
                        onValueChange = { onSeekTo((it * duration).toLong()) },
                        colors = SliderDefaults.colors(
                            thumbColor = theme.accentColor,
                            activeTrackColor = theme.accentColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    Row(Modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatTime(displayPos), color = Color.Gray, fontSize = 11.sp)
                        Text(formatTime(duration), color = Color.Gray, fontSize = 11.sp)
                    }
                }

                // Transport
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onSkipPrevious) {
                        Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(theme.accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onTogglePlayPause) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                "Play/Pause",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    IconButton(onClick = onSkipNext) {
                        Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }

                // Lyrics with tracking
                LyricsPanel(
                    song = song,
                    positionMs = displayPos,
                    isLoading = lyricsLoading,
                    onFetchLyrics = onFetchLyrics,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
