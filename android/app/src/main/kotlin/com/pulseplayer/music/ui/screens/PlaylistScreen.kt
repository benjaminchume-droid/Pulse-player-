package com.pulseplayer.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.pulseplayer.music.data.Playlist
import com.pulseplayer.music.data.Song
import com.pulseplayer.music.ui.components.GlassCard
import com.pulseplayer.music.ui.theme.*
import com.pulseplayer.music.viewmodel.PlaybackViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlaylistScreen(
    viewModel: PlaybackViewModel,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    var playlistName by remember { mutableStateOf("") }
    var aiPrompt by remember { mutableStateOf("") }
    var isAiGenerating by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Playlists Banner
        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(
                text = "PULSE MIXTURES",
                color = TextMuted,
                style = Typography.labelSmall
            )
            Text(
                text = "Dynamic Playlists",
                color = TextWhitePrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Smart AI Formulation Box Form
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Sparkles, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Text(
                        text = "FORMULATE SMART MIX_STREAMS",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        style = Typography.labelSmall
                    )
                }

                Text(
                    text = "Describe your mood to trigger the automated Pulse sound engineer synthesizer.",
                    color = TextGraySecondary,
                    fontSize = 11.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = aiPrompt,
                        onValueChange = { aiPrompt = it },
                        placeholder = { Text("Cyberpunk racing flow...", color = TextMuted, fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0x11FFFFFF),
                            unfocusedContainerColor = Color(0x05FFFFFF),
                            focusedIndicatorColor = NeonCyan,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (aiPrompt.isNotBlank() && songs.isNotEmpty()) {
                                isAiGenerating = true
                                coroutineScope.launch {
                                    delay(2000) // Simulate fast synthesiser calculation
                                    // Take 3 random songs as mock target match
                                    val matchCount = 3.coerceAtMost(songs.size)
                                    val sampled = songs.shuffled().take(matchCount).map { it.id }
                                    viewModel.createPlaylist(
                                        name = aiPrompt.take(24).trim() + " Flow",
                                        description = "AI Synced session on mood: '$aiPrompt'",
                                        songIds = sampled
                                    )
                                    aiPrompt = ""
                                    isAiGenerating = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BackgroundDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(52.dp),
                        enabled = !isAiGenerating && songs.isNotEmpty()
                    ) {
                        if (isAiGenerating) {
                            CircularProgressIndicator(color = BackgroundDark, modifier = Modifier.size(18.dp))
                        } else {
                            Text(
                                "SYNC",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                style = Typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        // Manual Creation Form Row layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                placeholder = { Text("New playlist name...", color = TextMuted, fontSize = 12.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0x11FFFFFF),
                    unfocusedContainerColor = Color(0x05FFFFFF),
                    focusedIndicatorColor = NeonPurple,
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
                    if (playlistName.isNotBlank()) {
                        viewModel.createPlaylist(name = playlistName)
                        playlistName = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    "CREATE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    style = Typography.labelSmall
                )
            }
        }

        // Playlists scroll list
        Text(
            text = "MIXTURE ARCHIVES",
            color = TextMuted,
            style = Typography.labelSmall
        )

        if (playlists.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(playlists) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x0AFFFFFF))
                            .clickable {
                                // Filter and play songs inside this playlist
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
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x1AFFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (playlist.description.contains("AI")) Icons.Default.Sparkles else Icons.Default.List,
                                contentDescription = null,
                                tint = if (playlist.description.contains("AI")) NeonCyan else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                color = TextWhitePrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${playlist.songIds.size} Tracks • ${if (playlist.description.contains("AI")) "AI Synced" else "Offline Custom"}",
                                color = TextGraySecondary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Playlist",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        } else {
            // Empty state playlist cards
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
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = Color(0x1FFFFFFF),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Playlists Found",
                        color = TextWhitePrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        style = Typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Create your first offline mixture or request the AI helper to synthesize a customized stream.",
                        color = TextGraySecondary,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
