package com.pulseplayer.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulseplayer.music.ui.components.GlassCard
import com.pulseplayer.music.ui.theme.*
import com.pulseplayer.music.viewmodel.PlaybackViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MixesScreen(
    viewModel: PlaybackViewModel,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val activeRealm by RealmManager.currentTheme.collectAsState()

    var aiPrompt by remember { mutableStateOf("") }
    var isAiGenerating by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // 8 Predefined Mix categories with futuristic descriptors
    val categories = listOf(
        MixCategory("Chill", "🌌", "Ambient downtempo & low-fidelity cyber acoustic streams.", Brush.linearGradient(colors = listOf(Color(0xFF0F2027), Color(0xFF203A43)))),
        MixCategory("Energy", "⚡", "High-voltage neural synths and industrial baseline drives.", Brush.linearGradient(colors = listOf(Color(0xFF780D21), Color(0xFFC31432)))),
        MixCategory("Night Drive", "🌃", "Synthwave neon skylines echoing through obsidian expressways.", Brush.linearGradient(colors = listOf(Color(0xFF1F1C2C), Color(0xFF928DAB)))),
        MixCategory("Focus", "🌀", "Alpha brainwave deep binaural filters for cognitive workspace acceleration.", Brush.linearGradient(colors = listOf(Color(0xFF0D1B2A), Color(0xFF415A77)))),
        MixCategory("Bass Boost", "🔊", "Heavily equalized low-frequency shockwaves to rattle your visualizer.", Brush.linearGradient(colors = listOf(Color(0xFF310F4E), Color(0xFFFF0D7F)))),
        MixCategory("Synthwave", "🕹️", "80s outrun vapor networks with retro holographic grids.", Brush.linearGradient(colors = listOf(Color(0xFF0D0221), Color(0xFF0F0C1B)))),
        MixCategory("Ambient", "🪐", "Isolated deep-space pads floating inside orbital nebulas.", Brush.linearGradient(colors = listOf(Color(0xFF243B55), Color(0xFF141E30)))),
        MixCategory("Cinematic", "🎭", "Orchestral audio architecture creating grand futuristic atmospheres.", Brush.linearGradient(colors = listOf(Color(0xFF240B36), Color(0xFFC31432))))
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Headers
        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(
                text = "PULSE AUDIOVERSE",
                color = activeRealm.accentColor,
                style = Typography.labelSmall
            )
            Text(
                text = "Curated Mix Realms",
                color = if (activeRealm.isLight) Color.Black else Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Selected/Active Mix Dynamic Banner
        if (selectedCategory != null) {
            val cat = categories.firstOrNull { it.name == selectedCategory }
            if (cat != null) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cat.brush),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cat.emoji, fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cat.name.uppercase() + " WORLD",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = cat.desc,
                                color = TextGraySecondary,
                                fontSize = 10.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { selectedCategory = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Deactivate Mix",
                                tint = Color.LightGray
                            )
                        }
                    }
                }
            }
        }

        // AI Formula Form
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = activeRealm.accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "FORMULATE CYBERNETIC MIX",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        style = Typography.labelSmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = aiPrompt,
                        onValueChange = { aiPrompt = it },
                        placeholder = { Text("Describe a soundscape mood...", color = TextMuted, fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0x11FFFFFF),
                            unfocusedContainerColor = Color(0x05FFFFFF),
                            focusedIndicatorColor = activeRealm.accentColor,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (aiPrompt.isNotBlank()) {
                                isAiGenerating = true
                                coroutineScope.launch {
                                    delay(1500) // Synthesize
                                    val sampled = songs.shuffled().take(3).map { it.id }
                                    viewModel.createPlaylist(
                                        name = aiPrompt.take(20).trim() + " Mix",
                                        description = "AI Synced on: '$aiPrompt'",
                                        songIds = sampled
                                    )
                                    aiPrompt = ""
                                    isAiGenerating = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeRealm.accentColor,
                            contentColor = if (activeRealm.isLight) Color.White else Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp),
                        enabled = !isAiGenerating
                    ) {
                        Text("SYNTH", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Horizontal Carousel of Genre Categories Worlds
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "WORLD CATEGORIES [SELECT SPACE]",
                color = TextMuted,
                style = Typography.labelSmall
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSel = selectedCategory == cat.name
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(cat.brush)
                            .border(
                                width = if (isSel) 2.dp else 0.dp,
                                color = if (isSel) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                selectedCategory = cat.name
                                // Filter songs containing genre or title match
                                val related = songs.filter {
                                    it.genre.lowercase().contains(cat.name.lowercase()) ||
                                            it.title.lowercase().contains(cat.name.lowercase())
                                }
                                val sampleIds = if (related.isNotEmpty()) {
                                    related.map { it.id }
                                } else {
                                    songs.shuffled().take(3).map { it.id }
                                }

                                if (sampleIds.isNotEmpty()) {
                                    viewModel.createPlaylist(
                                        name = "${cat.name} Stream",
                                        description = "Atmospheric ${cat.name} World session",
                                        songIds = sampleIds
                                    )
                                }
                            }
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cat.emoji, fontSize = 28.sp)
                            Column {
                                Text(
                                    text = cat.name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Realms Active",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active archives list
        Text(
            text = "ACTIVE MIXTURE ARCHIVES",
            color = TextMuted,
            style = Typography.labelSmall
        )

        val mixesPlaylists = playlists.filter {
            it.description.contains("AI") ||
                    it.description.contains("Atmospheric") ||
                    it.description.contains("session")
        }

        if (mixesPlaylists.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(mixesPlaylists) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x0AFFFFFF))
                            .clickable {
                                val playlistSongs = songs.filter { playlist.songIds.contains(it.id) }
                                if (playlistSongs.isNotEmpty()) {
                                    viewModel.playSong(playlistSongs, playlistSongs.first())
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x11FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = activeRealm.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                color = if (activeRealm.isLight) Color.Black else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = playlist.description,
                                color = TextGraySecondary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play immediately",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No formulated mix sessions yet. Formulate or select a World Category above to initiate.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

data class MixCategory(
    val name: String,
    val emoji: String,
    val desc: String,
    val brush: Brush
)
