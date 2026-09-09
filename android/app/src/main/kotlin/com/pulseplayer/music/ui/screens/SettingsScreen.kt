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
import com.pulseplayer.music.ui.components.UpdateSection
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
    val isEnriching by viewModel.isEnriching.collectAsState()
    val scrollState = rememberScrollState()

    val activeRealm by RealmManager.currentTheme.collectAsState()
    val performanceMode by RealmManager.performanceMode.collectAsState()
    val amoledMode by RealmManager.amoledMode.collectAsState()

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

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "SOFTWARE UPDATES",
                color = if (activeRealm.isLight) Color.DarkGray else Color.LightGray,
                style = Typography.labelSmall
            )
            UpdateSection()
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "LIBRARY METADATA",
                color = if (activeRealm.isLight) Color.DarkGray else Color.LightGray,
                style = Typography.labelSmall
            )
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enrich tags for local / downloaded songs",
                        color = if (activeRealm.isLight) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        "Reads embedded ID3 tags and fills gaps via MusicBrainz (title, artist, album, year).",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    Text("${songs.size} tracks in library", color = Color.Gray, fontSize = 11.sp)
                    Button(
                        onClick = { viewModel.enrichAllMetadata() },
                        enabled = !isEnriching,
                        colors = ButtonDefaults.buttonColors(containerColor = activeRealm.accentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isEnriching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enriching…", fontSize = 12.sp)
                        } else {
                            Text("Enrich all metadata", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "PULSE REALMS ACTIVE INTERFACE",
                color = activeRealm.accentColor,
                fontWeight = FontWeight.Bold,
                style = Typography.labelSmall
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                items(RealmManager.realms) { realm ->
                    val isSelected = activeRealm.id == realm.id
                    val cardBorder = if (isSelected) realm.accentColor else Color.White.copy(alpha = 0.12f)
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .height(140.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) realm.gradientColors[0] else Color(0x19FFFFFF)
                            )
                            .border(if (isSelected) 2.dp else 1.dp, cardBorder, RoundedCornerShape(20.dp))
                            .clickable { RealmManager.selectTheme(realm.id) }
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(text = realm.glyphEmblems, fontSize = 16.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(realm.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(realm.soundstageName, color = Color.LightGray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("GPU HARDWARE ACCELERATION PROFILE", color = Color.LightGray, style = Typography.labelSmall)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Atmospheric Rendering Level", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                Text(mode.name, color = if (isSel) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("AMOLED OLED POWER CONSERVATION", color = Color.LightGray, style = Typography.labelSmall)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AMOLED True-Blackout Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Force pixels off on OLED panels to save battery.", color = Color.Gray, fontSize = 11.sp)
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

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("COMMUNICATION LINK", color = Color.LightGray, style = Typography.labelSmall)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Bluetooth Audio Output", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(if (activeBtDevice != null) "Connected: $activeBtDevice" else btStatusText, color = if (activeBtDevice != null) activeRealm.accentColor else Color.Gray, fontSize = 11.sp)
                    }
                    if (btScanning) {
                        CircularProgressIndicator(color = activeRealm.accentColor, modifier = Modifier.size(20.dp))
                    } else {
                        Button(
                            onClick = {
                                btScanning = true
                                btStatusText = "Scanning…"
                                scope.launch {
                                    delay(2000)
                                    activeBtDevice = "Nothing Ear (a)"
                                    btStatusText = "Connected"
                                    btScanning = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = activeRealm.accentColor, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("SCAN", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ABOUT PULSE PLAYER", color = Color.LightGray, style = Typography.labelSmall)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Info, null, tint = Color.White)
                    Column {
                        Text("Pulse Player Native Client", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Version 1.0.2 (Kotlin/Compose)", color = Color.Gray, fontSize = 11.sp)
                        Text("Updates · Lyrics · Metadata enrichment", color = Color.Gray, fontSize = 9.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(140.dp))
    }
}
