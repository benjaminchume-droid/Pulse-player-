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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulseplayer.music.data.Song
import com.pulseplayer.music.ui.components.GlassCard
import com.pulseplayer.music.ui.theme.*
import com.pulseplayer.music.viewmodel.PlaybackViewModel
import java.util.*

@Composable
fun HomeScreen(
    viewModel: PlaybackViewModel,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val userLevel by viewModel.userLevel.collectAsState()
    val userXp by viewModel.userXp.collectAsState()
    val streak by viewModel.listeningStreak.collectAsState()

    // Bind to the active global realms system
    val theme by RealmManager.currentTheme.collectAsState()

    val scrollState = rememberScrollState()

    // Determine featured song
    val favorites = songs.filter { it.isFavorite }
    val featuredSong = favorites.firstOrNull() ?: songs.firstOrNull()

    val textPrimaryColor = if (theme.isLight) Color.Black else Color.White
    val textMutedColor = if (theme.isLight) Color.DarkGray.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.5f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Dynamic Greeting
        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(
                text = "PULSE VISION REALMS",
                color = theme.accentColor,
                fontWeight = FontWeight.Bold,
                style = Typography.labelSmall
            )
            Text(
                text = "${getGreeting()}, Explorer",
                color = textPrimaryColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light
            )
        }

        // Hero Dynamic Glasscard for Featured Audio Track
        if (featuredSong != null) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { viewModel.playSong(songs, featuredSong) }
            ) {
                // Background visual highlights
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(theme.accentColor.copy(alpha = 0.25f), Color.Transparent)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = theme.glyphEmblems, fontSize = 12.sp)
                        Text(
                            text = "AMBISONIC CORE READY [${theme.soundstageName.uppercase()}]",
                            color = textMutedColor,
                            style = Typography.labelSmall
                        )
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = featuredSong.title,
                            color = textPrimaryColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Light,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = featuredSong.artist,
                            color = theme.accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { viewModel.playSong(songs, featuredSong) },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor, contentColor = if (theme.isLight) Color.White else Color.Black),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "STREAM CHRONIC CORE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            style = Typography.labelSmall
                        )
                    }
                }
            }
        } else {
            // Null state layout
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🛰️", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Track Synchronized",
                        color = textPrimaryColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Please tap scan files inside Library to register media tracks.",
                        color = textMutedColor,
                        fontSize = 10.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Streak Progress Quest card
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LEVEL $userLevel EXPLORER / EXP",
                    color = theme.accentColor,
                    fontWeight = FontWeight.Bold,
                    style = Typography.labelSmall
                )
                Text(
                    text = "${streak}D LISTENING STREAK",
                    color = theme.glowColor,
                    fontWeight = FontWeight.Bold,
                    style = Typography.labelSmall
                )
            }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Realms Synchronization Quest Progress",
                            color = textPrimaryColor,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "$userXp / 1000 XP",
                            color = textPrimaryColor,
                            style = Typography.labelSmall
                        )
                    }
                    // Progress Track Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (theme.isLight) Color(0x33000000) else Color(0x33FFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = (userXp.toFloat() / 1000f).coerceIn(0f, 1f))
                                .background(theme.accentColor)
                        )
                    }
                }
            }
        }

        // Recent Files scanned loop list
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "RECENT CORE SENSOR READINGS",
                color = textPrimaryColor,
                style = Typography.labelSmall
            )

            if (songs.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(songs.take(10)) { song ->
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable { viewModel.playSong(songs, song) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (theme.isLight) Color(0x11000000) else Color(0x1AFFFFFF))
                                    .border(1.dp, theme.accentColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = theme.glyphEmblems, fontSize = 36.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = song.title,
                                color = textPrimaryColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                color = theme.accentColor,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "No newly tracked files. Scanner index empty.",
                    color = textMutedColor,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        // Favorites Range List
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(14.dp))
                Text(
                    text = "FAVORITES INTEGRATION FIELD",
                    color = textPrimaryColor,
                    style = Typography.labelSmall
                )
            }

            if (favorites.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    favorites.take(5).forEach { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (theme.isLight) Color(0x06000000) else Color(0x0AFFFFFF))
                                .border(1.dp, theme.accentColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .clickable { viewModel.playSong(songs, song) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(theme.accentColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🎧", fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    color = textPrimaryColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    color = theme.accentColor,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (theme.isLight) Color(0x05000000) else Color(0x05FFFFFF))
                        .border(1.dp, theme.accentColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Favorites Tracked Yet",
                        color = textMutedColor,
                        fontSize = 12.sp,
                        style = Typography.labelSmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..4 -> "Good Astral Night"
        in 5..11 -> "Good Solar Morning"
        in 12..16 -> "Good Solar Afternoon"
        else -> "Good Nebula Evening"
    }
}
