package com.pulseplayer.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pulseplayer.music.data.RepeatMode
import com.pulseplayer.music.data.Song

@Composable
fun NowPlayingOverlay(
    isOpen: Boolean,
    song: Song?,
    isPlaying: Boolean,
    playbackPosition: Long,
    lyricsLoading: Boolean,
    repeatMode: RepeatMode,
    fullScreenLyrics: Boolean,
    onFullScreenLyrics: (Boolean) -> Unit,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onFetchLyrics: () -> Unit,
    onEnrichMetadata: () -> Unit,
    onCycleRepeat: () -> Unit,
    onQueueNext: () -> Unit,
    onSleepTimer: (Int) -> Unit
) {
    AnimatedVisibility(
        visible = isOpen && song != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val s = song ?: return@AnimatedVisibility
        val duration = s.duration.coerceAtLeast(1L)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF00A0A12))
        ) {
            if (s.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = s.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(modifier = Modifier.fillMaxSize().background(Color(0xCC0A0A12)))
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.KeyboardArrowDown, "Close", tint = Color.White)
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (s.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Favorite",
                            tint = if (s.isFavorite) Color(0xFFFF5252) else Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                AsyncImage(
                    model = s.coverUrl.ifBlank { null },
                    contentDescription = "Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                )
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    s.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(s.artist, color = Color.White.copy(0.7f), fontSize = 16.sp)
                if (s.album.isNotBlank() && s.album != "Unknown Album") {
                    Text(s.album, color = Color.White.copy(0.5f), fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Slider(
                    value = (playbackPosition.toFloat() / duration).coerceIn(0f, 1f),
                    onValueChange = { onSeekTo((it * duration).toLong()) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatMs(playbackPosition), color = Color.Gray, fontSize = 12.sp)
                    Text(formatMs(duration), color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onCycleRepeat) {
                        Icon(
                            Icons.Default.Repeat,
                            "Repeat",
                            tint = if (repeatMode != RepeatMode.OFF) Color(0xFF7C4DFF) else Color.White
                        )
                    }
                    IconButton(onClick = onSkipPrevious) {
                        Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF1a1a2e),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    IconButton(onClick = onSkipNext) {
                        Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    TextButton(onClick = onFetchLyrics, enabled = !lyricsLoading) {
                        Text(if (lyricsLoading) "…" else "Lyrics", color = Color.White, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onEnrichMetadata) {
                    Text("Identify / Enrich metadata", color = Color(0xFFB39DDB), fontSize = 13.sp)
                }
                if (fullScreenLyrics && s.lyrics.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        s.lyrics,
                        color = Color.White.copy(0.85f),
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                } else if (s.lyrics.isNotBlank()) {
                    TextButton(onClick = { onFullScreenLyrics(true) }) {
                        Text("Show lyrics", color = Color.White.copy(0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
