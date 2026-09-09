package com.pulseplayer.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import com.pulseplayer.music.sources.SourceRegistry
import com.pulseplayer.music.ui.theme.RealmManager
import com.pulseplayer.music.viewmodel.PlaybackViewModel

@Composable
fun SearchScreen(
    viewModel: PlaybackViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val theme by RealmManager.currentTheme.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val localSongs by viewModel.songs.collectAsState()
    var query by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf<String?>(null) }

    val localMatches = remember(query, localSongs) {
        if (query.isBlank()) emptyList()
        else localSongs.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
        }.take(30)
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
            }
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.length >= 2) viewModel.searchOnline(it, selectedSource)
                },
                placeholder = { Text("Search library & online…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Sources", color = theme.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            item {
                FilterChip(
                    selected = selectedSource == null,
                    onClick = {
                        selectedSource = null
                        if (query.isNotBlank()) viewModel.searchOnline(query, null)
                    },
                    label = { Text("All") }
                )
            }
            items(SourceRegistry.all) { src ->
                FilterChip(
                    selected = selectedSource == src.id,
                    onClick = {
                        selectedSource = src.id
                        if (query.isNotBlank()) viewModel.searchOnline(query, src.id)
                    },
                    label = { Text(src.displayName) }
                )
            }
        }

        if (isSearching) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = theme.accentColor)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
            if (localMatches.isNotEmpty()) {
                item { Text("ON THIS DEVICE", color = theme.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                items(localMatches) { song ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x0AFFFFFF))
                            .clickable { viewModel.playSong(localSongs, song) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(song.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }
            if (results.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ONLINE", color = theme.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                items(results) { r ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x0AFFFFFF))
                            .clickable { viewModel.playStreamResult(r) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(r.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                            Text("${r.artist} · ${r.source}", color = theme.accentColor, fontSize = 11.sp)
                            Text(
                                if (r.canStreamInApp) "Tap to stream" else "Opens externally",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
