package com.pulseplayer.music.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pulseplayer.music.data.Song
import com.pulseplayer.music.viewmodel.DownloadState
import com.pulseplayer.music.viewmodel.PlaybackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingOverlay(
    song: Song?,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onPlayPause: () -> Unit,
    playbackViewModel: PlaybackViewModel = viewModel()
) {
    val context = LocalContext.current
    val downloadState by playbackViewModel.downloadState.collectAsState()
    val isDownloading = downloadState !is DownloadState.Idle && downloadState !is DownloadState.Completed && downloadState !is DownloadState.Failed

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Album Art
            song?.let { currentSong ->
                AsyncImage(
                    model = currentSong.albumArt,
                    contentDescription = "Album art for ${currentSong.title}",
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                )

                // Song Info
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = currentSong.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 22.sp
                    )
                    Text(
                        text = currentSong.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                }

                // Download Progress
                AnimatedVisibility(
                    visible = downloadState !is DownloadState.Idle,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 })
                ) {
                    DownloadProgressSection(downloadState)
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Download Button
                    DownloadButton(
                        isDownloading = isDownloading,
                        downloadState = downloadState,
                        onDownloadClick = {
                            currentSong.let { track ->
                                playbackViewModel.downloadTrack(track)
                                Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    // Play/Pause Button
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Icon(
                            painter = androidx.compose.material.icons.Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF1a1a2e),
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Dismiss Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            painter = androidx.compose.material.icons.Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Dismiss",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadProgressSection(downloadState: DownloadState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (downloadState) {
            is DownloadState.Queued -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Color(0xFF4ade80),
                    strokeWidth = 4.dp
                )
                Text(
                    text = "Queued...",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
            is DownloadState.Downloading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color(0xFF3b82f6),
                        strokeWidth = 4.dp,
                        progress = downloadState.progress / 100f
                    )
                    Text(
                        text = "${downloadState.progress}%",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            is DownloadState.Writing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Color(0xFFf59e0b),
                    strokeWidth = 4.dp
                )
                Text(
                    text = "Writing to disk...",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
            is DownloadState.Completed -> {
                Icon(
                    painter = androidx.compose.material.icons.Icons.Filled.CheckCircle,
                    contentDescription = "Download complete",
                    tint = Color(0xFF4ade80),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Download complete!",
                    color = Color(0xFF4ade80),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            is DownloadState.Failed -> {
                Icon(
                    painter = androidx.compose.material.icons.Icons.Filled.Error,
                    contentDescription = "Download failed",
                    tint = Color(0xFFef4444),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = downloadState.message ?: "Download failed",
                    color = Color(0xFFef4444),
                    fontSize = 14.sp
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun DownloadButton(
    isDownloading: Boolean,
    downloadState: DownloadState,
    onDownloadClick: () -> Unit
) {
    val buttonColor = when (downloadState) {
        is DownloadState.Completed -> Color(0xFF4ade80)
        is DownloadState.Failed -> Color(0xFFef4444)
        else -> Color.White.copy(alpha = 0.2f)
    }

    IconButton(
        onClick = onDownloadClick,
        enabled = !isDownloading,
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(buttonColor)
    ) {
        Icon(
            painter = when (downloadState) {
                is DownloadState.Completed -> androidx.compose.material.icons.Icons.Filled.CheckCircle
                is DownloadState.Failed -> androidx.compose.material.icons.Icons.Filled.Error
                else -> androidx.compose.material.icons.Icons.Filled.Download
            },
            contentDescription = "Download",
            tint = if (isDownloading) Color.White.copy(alpha = 0.5f) else Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}
