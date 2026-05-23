package com.pulseplayer.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulseplayer.music.ui.components.GlassCard
import com.pulseplayer.music.ui.theme.*
import com.pulseplayer.music.viewmodel.PlaybackViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: PlaybackViewModel,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val scrollState = rememberScrollState()

    var activeTheme by remember { mutableStateOf("cosmic") }
    
    // Bluetooth Headset connection states
    var btScanning by remember { mutableStateOf(false) }
    var btStatusText by remember { mutableStateOf("Disconnected") }
    var activeBtDevice by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Headers
        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(
                text = "PULSE CONTROL",
                color = TextMuted,
                style = Typography.labelSmall
            )
            Text(
                text = "System Settings",
                color = TextWhitePrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Bluetooth Audio Pairing Module
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "COMMUNICATION LINK",
                color = TextMuted,
                style = Typography.labelSmall
            )
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Bluetooth Audio Output",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (activeBtDevice != null) "Connected: $activeBtDevice" else btStatusText,
                                color = if (activeBtDevice != null) NeonCyan else TextGraySecondary,
                                fontSize = 11.sp
                            )
                        }

                        if (btScanning) {
                            CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(20.dp))
                        } else {
                            Button(
                                onClick = {
                                    btScanning = true
                                    btStatusText = "Scanning for devices..."
                                    activeBtDevice = null
                                    scope.launch {
                                        delay(2500) // Simulate headset discovery
                                        activeBtDevice = "Nothing Ear (a)"
                                        btStatusText = "Connected"
                                        btScanning = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BackgroundDark),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "SCAN",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    style = Typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // Palette Themes selector
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "VISUAL INTERFACE ENVIRONMENT",
                color = TextMuted,
                style = Typography.labelSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val themesList = listOf(
                    "cosmic" to "Midnight Cyan",
                    "cyberpunk" to "Neon Grid",
                    "monochrome" to "Onyx Black"
                )

                themesList.forEach { (key, title) ->
                    val isActive = activeTheme == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isActive) Color(0x32D600FF) else Color(0x0AFFFFFF))
                            .clickable { activeTheme = key }
                            .padding(12.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Text(
                                text = if (isActive) "● ACTIVE" else "INACTIVE",
                                color = if (isActive) NeonCyan else TextMuted,
                                fontSize = 8.sp,
                                style = Typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Library Index and Diagnostics
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "STORAGE ENGINE METRICS",
                color = TextMuted,
                style = Typography.labelSmall
            )
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Scanned File Sensors", color = TextGraySecondary, fontSize = 12.sp)
                        Text("${songs.size} Audio Tracks", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Divider(color = Color(0x19FFFFFF))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Audio Backend Engine", color = TextGraySecondary, fontSize = 12.sp)
                        Text("MediaPlayer API", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Divider(color = Color(0x19FFFFFF))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cache Database", color = TextGraySecondary, fontSize = 12.sp)
                        Text("Room DB (SQLite)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Application Credits
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "ABOUT PULSE PLAYER",
                color = TextMuted,
                style = Typography.labelSmall
            )
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x19FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.White)
                    }
                    Column {
                        Text(
                            text = "Pulse Player Native client",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Version 1.0.0 (Kotlin/Compose)",
                            color = TextGraySecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Developed with Jetpack Compose standard MVVM architectural blocks.",
                            color = TextMuted,
                            fontSize = 9.sp,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(140.dp))
    }
}
