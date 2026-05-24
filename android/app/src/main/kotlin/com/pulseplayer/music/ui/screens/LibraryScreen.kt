package com.pulseplayer.music.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulseplayer.music.data.Song
import com.pulseplayer.music.ui.components.GlassCard
import com.pulseplayer.music.ui.theme.*
import com.pulseplayer.music.viewmodel.PlaybackViewModel

@Composable
fun LibraryScreen(
    viewModel: PlaybackViewModel,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    // Determine target permission matrices based on Android SDK level
    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    var showExplanationDialog by remember { mutableStateOf(false) }
    var wasPermissionDenied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val anyGranted = results.values.any { it }
        if (anyGranted) {
            wasPermissionDenied = false
            showExplanationDialog = false
            viewModel.scanDeviceAudio()
        } else {
            wasPermissionDenied = true
            showExplanationDialog = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Library Headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PULSE STORAGE",
                    color = TextMuted,
                    style = Typography.labelSmall
                )
                Text(
                    text = "Library Tracks",
                    color = TextWhitePrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Spinner/Icon scan trigger button
            val infiniteTransition = rememberInfiniteTransition(label = "RadarScannerRotation")
            val angle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "RotateSpinner"
            )

            Button(
                onClick = { launcher.launch(permissionsToRequest) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Scan Audio Files",
                    modifier = Modifier
                        .size(16.dp)
                        .then(if (isScanning) Modifier.rotate(angle) else Modifier)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isScanning) "SCANNING..." else "SCAN MEDIA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    style = Typography.labelSmall
                )
            }
        }

        if (showExplanationDialog) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚠️", fontSize = 18.sp)
                        Text(
                            text = "STORAGE PERMISSION REQUIRED",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            style = Typography.labelSmall
                        )
                    }
                    Text(
                        text = "Pulse Player operates natively on your local device. To map, cache, and play your audio tracks, we need authorization to access your media library files. Without this, no tracks can be indexed.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showExplanationDialog = false }) {
                            Text("DISMISS", color = Color.White.copy(0.6f), fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { launcher.launch(permissionsToRequest) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("AUTHORIZE SCAN", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Library Song items
        if (songs.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(songs) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x0AFFFFFF))
                            .clickable { viewModel.playSong(songs, song) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x22FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🎵", fontSize = 20.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = TextWhitePrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                color = TextGraySecondary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = { viewModel.toggleFavorite(song) }) {
                            Icon(
                                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite Toggle",
                                tint = if (song.isFavorite) NeonCyan else Color(0x66FFFFFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // High fidelity Null State Layout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0x11FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0x33FFFFFF),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Library is Empty",
                        color = TextWhitePrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pulse scanned index is empty. Tap SCAN MEDIA above to request storage access permissions and map all audio signals on your device.",
                        color = TextGraySecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
