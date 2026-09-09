package com.pulseplayer.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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

@Composable
fun SettingsScreen(
    viewModel: PlaybackViewModel,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val isEnriching by viewModel.isEnriching.collectAsState()
    val crossfade by viewModel.crossfadeSeconds.collectAsState()
    val scrollState = rememberScrollState()

    val activeRealm by RealmManager.currentTheme.collectAsState()
    val performanceMode by RealmManager.performanceMode.collectAsState()
    val amoledMode by RealmManager.amoledMode.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Text("SETTINGS", color = Color.Gray, style = Typography.labelSmall)
            Text(
                "Pulse Control",
                color = if (activeRealm.isLight) Color.Black else Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light
            )
        }

        Text("SOFTWARE UPDATES", color = Color.Gray, style = Typography.labelSmall)
        UpdateSection()

        Text("PLAYBACK", color = Color.Gray, style = Typography.labelSmall)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Crossfade", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "Start fading toward the next track before the current one ends.",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                Text(
                    if (crossfade == 0) "Off" else "$crossfade seconds",
                    color = activeRealm.accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0, 5, 10, 15, 20, 25).forEach { sec ->
                        val sel = crossfade == sec
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) activeRealm.accentColor else Color(0x14FFFFFF))
                                .clickable { viewModel.setCrossfadeSeconds(sec) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (sec == 0) "Off" else "${sec}s",
                                color = if (sel) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Text("LIBRARY METADATA & LYRICS", color = Color.Gray, style = Typography.labelSmall)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Enrich all tracks",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    "Reads embedded tags + MusicBrainz, then fetches lyrics (LRCLIB) when missing.",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                Text("${songs.size} tracks", color = Color.Gray, fontSize = 11.sp)
                Button(
                    onClick = { viewModel.enrichAllMetadata() },
                    enabled = !isEnriching,
                    colors = ButtonDefaults.buttonColors(containerColor = activeRealm.accentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isEnriching) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Enriching…", fontSize = 12.sp)
                    } else {
                        Text("Enrich metadata + lyrics", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Text("REALMS", color = activeRealm.accentColor, fontWeight = FontWeight.Bold, style = Typography.labelSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(RealmManager.realms) { realm ->
                val isSelected = activeRealm.id == realm.id
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) realm.gradientColors[0] else Color(0x19FFFFFF))
                        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) realm.accentColor else Color.White.copy(0.12f), RoundedCornerShape(20.dp))
                        .clickable { RealmManager.selectTheme(realm.id) }
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Text(realm.glyphEmblems, fontSize = 16.sp)
                        Spacer(Modifier.weight(1f))
                        Text(realm.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(realm.soundstageName, color = Color.LightGray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        Text("PERFORMANCE", color = Color.Gray, style = Typography.labelSmall)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("AMOLED black", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("True black backgrounds on OLED.", color = Color.Gray, fontSize = 11.sp)
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

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Info, null, tint = Color.White)
                Column {
                    Text("Pulse Player", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Version 1.0.3 · Velocity Lab / Glass Line", color = Color.Gray, fontSize = 11.sp)
                    Text("Streaming plugins · Lyrics · Metadata · MIT", color = Color.Gray, fontSize = 9.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
