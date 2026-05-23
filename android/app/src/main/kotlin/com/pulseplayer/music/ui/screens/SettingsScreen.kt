package com.pulseplayer.music.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
    
    // Bind to our brand new global Realm state engine
    val activeRealm by RealmManager.currentTheme.collectAsState()
    val performanceMode by RealmManager.performanceMode.collectAsState()
    val amoledMode by RealmManager.amoledMode.collectAsState()
    
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
                text = "SYSTEM SETTINGS",
                color = if (activeRealm.isLight) Color.DarkGray else Color.LightGray,
                style = Typography.labelSmall
            )
            Text(
                text = "Pulse Control Center",
                color = if (activeRealm.isLight) Color.Black else Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // 1. PULSE REALMS CAROUSEL MODULE
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PULSE REALMS ACTIVE INTERFACE",
                    color = activeRealm.accentColor,
                    fontWeight = FontWeight.Bold,
                    style = Typography.labelSmall
                )
                Text(
                    text = "15 DIMENSIONS AVAILABLE",
                    color = if (activeRealm.isLight) Color.DarkGray else Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Horizontal Theme Preview Matrix Slider
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                items(RealmManager.realms) { realm ->
                    val isSelected = activeRealm.id == realm.id

                    // Elastic sizing transitions for Center-Focus effect
                    val scaleFactor by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 0.95f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "ScaleFactor"
                    )

                    val cardBorder = if (isSelected) {
                        realm.accentColor
                    } else {
                        if (activeRealm.isLight) Color.LightGray.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f)
                    }

                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(140.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) realm.gradientColors[0]
                                else (if (activeRealm.isLight) Color(0x35E2E8F0) else Color(0x19FFFFFF))
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = cardBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { RealmManager.selectTheme(realm.id) }
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Emblem glyph header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(realm.accentColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = realm.glyphEmblems, fontSize = 16.sp)
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(realm.accentColor)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            color = if (realm.isLight) Color.White else Color.Black,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = realm.name,
                                color = if (isSelected || !activeRealm.isLight) Color.White else Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = realm.soundstageName,
                                color = if (isSelected || !activeRealm.isLight) Color.LightGray else Color.DarkGray,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Descriptive readout for active selection
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "REALM IDENTITY SPECTROMETER",
                        color = activeRealm.accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        style = Typography.labelSmall
                    )
                    Text(
                        text = activeRealm.description,
                        color = if (activeRealm.isLight) Color.Black else Color.White,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("Atmospheric Deck", color = Color.Gray, fontSize = 9.sp)
                            Text(if (activeRealm.atmosphericDepth) "CONNECTED" else "STANDBY", color = activeRealm.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Motion Physics", color = Color.Gray, fontSize = 9.sp)
                            Text(activeRealm.motionStyle.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Visualizer Preset", color = Color.Gray, fontSize = 9.sp)
                            Text(activeRealm.visualizerStyle.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. RENDERING HARDWARE PERFORMANCE SELECTOR
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "GPU HARDWARE ACCELERATION PROFILE",
                color = if (activeRealm.isLight) Color.DarkGray else Color.LightGray,
                style = Typography.labelSmall
            )
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Atmospheric Rendering Level", color = if (activeRealm.isLight) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Optimize particle buffers & gas sweeps", color = Color.Gray, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(activeRealm.accentColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(performanceMode.name, color = activeRealm.accentColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }

                    // Selector buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PerformanceMode.values().forEach { mode ->
                            val isSel = performanceMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) activeRealm.accentColor else Color(0x0CFFFFFF))
                                    .clickable { RealmManager.setPerformanceMode(mode) }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.name,
                                    color = if (isSel) (if (activeRealm.isLight) Color.White else Color.Black) else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. AMOLED BLACKOUT SYSTEM
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "AMOLED OLED POWER CONSERVATION",
                color = if (activeRealm.isLight) Color.DarkGray else Color.LightGray,
                style = Typography.labelSmall
            )
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AMOLED True-Blackout Mode",
                            color = if (activeRealm.isLight) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Disable color sweeps. Force pixels to off state on OLED panels to prolong battery.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                    Switch(
                        checked = amoledMode,
                        onCheckedChange = { RealmManager.setAmoledMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = activeRealm.accentColor,
                            checkedTrackColor = activeRealm.accentColor.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }

        // Bluetooth Audio Pairing Module
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "COMMUNICATION LINK",
                color = if (activeRealm.isLight) Color.DarkGray else Color.LightGray,
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
                                color = if (activeRealm.isLight) Color.Black else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (activeBtDevice != null) "Connected: $activeBtDevice" else btStatusText,
                                color = if (activeBtDevice != null) activeRealm.accentColor else Color.Gray,
                                fontSize = 11.sp
                            )
                        }

                        if (btScanning) {
                            CircularProgressIndicator(color = activeRealm.accentColor, modifier = Modifier.size(20.dp))
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
                                colors = ButtonDefaults.buttonColors(containerColor = activeRealm.accentColor, contentColor = if (activeRealm.isLight) Color.White else Color.Black),
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

        // Library Index and Diagnostics
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "STORAGE ENGINE METRICS",
                color = if (activeRealm.isLight) Color.DarkGray else Color.LightGray,
                style = Typography.labelSmall
            )
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Scanned File Sensors", color = Color.Gray, fontSize = 12.sp)
                        Text("${songs.size} Audio Tracks", color = if (activeRealm.isLight) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Divider(color = Color(0x19FFFFFF))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Audio Backend Engine", color = Color.Gray, fontSize = 12.sp)
                        Text("MediaPlayer API", color = activeRealm.accentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Divider(color = Color(0x19FFFFFF))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cache Database", color = Color.Gray, fontSize = 12.sp)
                        Text("Room DB (SQLite)", color = if (activeRealm.isLight) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Application Credits
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "ABOUT PULSE PLAYER",
                color = if (activeRealm.isLight) Color.DarkGray else Color.LightGray,
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
                        Icon(Icons.Default.Info, contentDescription = null, tint = if (activeRealm.isLight) Color.Black else Color.White)
                    }
                    Column {
                        Text(
                            text = "Pulse Player Native Client",
                            color = if (activeRealm.isLight) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Version 1.1.0-Realms (Kotlin/Compose)",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Developed with modular high frame-rate rendering pipelines.",
                            color = Color.Gray,
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
