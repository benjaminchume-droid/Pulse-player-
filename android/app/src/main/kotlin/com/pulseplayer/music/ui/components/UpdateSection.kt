package com.pulseplayer.music.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulseplayer.music.ui.theme.RealmManager
import com.pulseplayer.music.update.UpdateChecker
import com.pulseplayer.music.update.UpdateState

@Composable
fun UpdateSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val theme by RealmManager.currentTheme.collectAsState()
    val checker = remember { UpdateChecker(context.applicationContext) }
    val progress by checker.progress.collectAsState()

    DisposableEffect(Unit) {
        onDispose { checker.dispose() }
    }

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "App Updates",
                        color = if (theme.isLight) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        "Current: v${checker.currentVersionName()}",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
                Icon(
                    Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = theme.accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = progress.message.ifBlank {
                    when (progress.state) {
                        UpdateState.IDLE -> "Tap Check to look for a new release"
                        else -> progress.state.name
                    }
                },
                color = when (progress.state) {
                    UpdateState.ERROR -> Color(0xFFFF6B6B)
                    UpdateState.UP_TO_DATE -> Color(0xFF4ADE80)
                    UpdateState.AVAILABLE, UpdateState.READY_TO_INSTALL -> theme.accentColor
                    else -> Color.LightGray
                },
                fontSize = 12.sp
            )

            if (progress.state == UpdateState.DOWNLOADING || progress.state == UpdateState.PAUSED) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = progress.percent / 100f,
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = theme.accentColor,
                        trackColor = Color.White.copy(alpha = 0.12f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${progress.percent}%", color = theme.accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(progress.speedLabel, color = Color.Gray, fontSize = 12.sp)
                        val totalMb = if (progress.totalBytes > 0) String.format("%.1f MB", progress.totalBytes / 1_000_000.0) else "?"
                        val doneMb = String.format("%.1f MB", progress.bytesDownloaded / 1_000_000.0)
                        Text("$doneMb / $totalMb", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (progress.state) {
                    UpdateState.IDLE, UpdateState.UP_TO_DATE, UpdateState.ERROR -> {
                        Button(
                            onClick = { checker.checkForUpdate() },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier = Modifier.width(6.dp))
                            Text("Check for update", fontSize = 12.sp)
                        }
                    }
                    UpdateState.CHECKING -> {
                        CircularProgressIndicator(color = theme.accentColor, modifier = Modifier.size(28.dp))
                    }
                    UpdateState.AVAILABLE -> {
                        Button(
                            onClick = { checker.startDownload() },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier = Modifier.width(6.dp))
                            Text("Download ${progress.updateInfo?.versionName ?: ""}", fontSize = 12.sp)
                        }
                    }
                    UpdateState.DOWNLOADING -> {
                        OutlinedButton(
                            onClick = { checker.pause() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Pause, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier = Modifier.width(4.dp))
                            Text("Pause", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { checker.cancel() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }
                    }
                    UpdateState.PAUSED -> {
                        Button(
                            onClick = { checker.resume() },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier = Modifier.width(4.dp))
                            Text("Resume", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { checker.cancel() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }
                    }
                    UpdateState.READY_TO_INSTALL -> {
                        Button(
                            onClick = { checker.installDownloadedApk() },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Install update", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
