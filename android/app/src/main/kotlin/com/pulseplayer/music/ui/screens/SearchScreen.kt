package com.pulseplayer.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
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
import com.pulseplayer.music.data.Song
import com.pulseplayer.music.ui.theme.*
import com.pulseplayer.music.viewmodel.PlaybackViewModel

@Composable
fun SearchScreen(
    viewModel: PlaybackViewModel,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    var query by remember { mutableStateOf("") }

    // Instant query filtering matching text segments
    val filteredSongs = remember(query, songs) {
        if (query.isBlank()) emptyList()
        else {
            val lower = query.lowercase()
            songs.filter {
                it.title.lowercase().contains(lower) ||
                it.artist.lowercase().contains(lower) ||
                it.album.lowercase().contains(lower) ||
                it.genre.lowercase().contains(lower)
            }
        }
    }

    // Dynamic suggested genres and artists derived from library metadata
    val suggestedGenres = remember(songs) {
        songs.map { it.genre }.distinct().take(4)
    }
    val suggestedArtists = remember(songs) {
        songs.map { it.artist }.filter { it != "Unknown Artist" }.distinct().take(6)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Headers
        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(
                text = "PULSE SCANNER",
                color = TextMuted,
                style = Typography.labelSmall
            )
            Text(
                text = "Library Search",
                color = TextWhitePrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Search Input field
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Songs, Artists, Albums...", color = TextMuted, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search query", tint = Color.White)
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0D0D0D),
                unfocusedContainerColor = Color(0xFF0D0D0D),
                focusedIndicatorColor = Color(0x33FFFFFF),
                unfocusedIndicatorColor = Color(0x19FFFFFF),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            singleLine = true
        )

        if (songs.isEmpty()) {
            // Scanner disabled state notice
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
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0x22FFFFFF), modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Search Index Empty",
                        color = Color.White,
                        style = Typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your audio query catalog depends strictly on media scanning. Go to your Library tab to scan and load tracks first.",
                        color = TextGraySecondary,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            if (query.isNotEmpty()) {
                // Results mapping list
                Text(
                    text = "SEARCH RESULTS FOR \"${query.uppercase()}\"",
                    color = TextMuted,
                    style = Typography.labelSmall
                )

                if (filteredSongs.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(filteredSongs) { song ->
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
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x16FFFFFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎵", fontSize = 18.sp)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = Color.White,
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

                                IconButton(onClick = { viewModel.playSong(songs, song) }) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play immediately",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
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
                            text = "No matching tracks found",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            style = Typography.labelSmall
                        )
                    }
                }
            } else {
                // Default Suggestions Dashboard
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (suggestedGenres.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "EXPLORE GENRES",
                                color = TextMuted,
                                style = Typography.labelSmall
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                suggestedGenres.forEach { genre ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(80.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0x0EFFFFFF))
                                            .clickable { query = genre }
                                            .padding(12.dp),
                                        contentAlignment = Alignment.BottomStart
                                    ) {
                                        Text(
                                            text = genre,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (suggestedArtists.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "SUGGESTED ARTISTS",
                                color = TextMuted,
                                style = Typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Scrollable list of artists tags chips
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(suggestedArtists) { artist ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0x11FFFFFF))
                                            .clickable { query = artist }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = artist.uppercase(),
                                            color = TextGraySecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            style = Typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (suggestedGenres.isEmpty() && suggestedArtists.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Type a query above to scan tracks",
                                color = TextMuted,
                                fontSize = 11.sp,
                                style = Typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
