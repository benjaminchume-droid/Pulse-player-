package com.pulseplayer.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulseplayer.music.ui.theme.RealmManager
import com.pulseplayer.music.viewmodel.PlaybackViewModel

@Composable
fun PlaylistsScreen(viewModel: PlaybackViewModel, modifier: Modifier = Modifier) {
    val playlists by viewModel.playlists.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val theme by RealmManager.currentTheme.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    val albums = remember(songs) {
        songs.map { it.album }.filter { it.isNotBlank() && it != "Unknown Album" }.distinct().sorted()
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Playlists & Albums", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light)
            IconButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, "Create", tint = theme.accentColor)
            }
        }

        if (showCreate) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Playlist name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.createPlaylist(newName.trim())
                            newName = ""
                            showCreate = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)
                ) { Text("Create") }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
            if (playlists.isNotEmpty()) {
                item {
                    Text("YOUR PLAYLISTS", color = theme.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                items(playlists) { pl ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x12FFFFFF))
                            .clickable { viewModel.playPlaylist(pl) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = theme.accentColor)
                        Spacer(Modifier = Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(pl.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("${pl.songIds.size} tracks", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
            if (albums.isNotEmpty()) {
                item {
                    Spacer(Modifier = Modifier.height(12.dp))
                    Text("ALBUMS FROM LIBRARY", color = theme.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                items(albums) { album ->
                    val count = songs.count { it.album == album }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x0AFFFFFF))
                            .clickable { viewModel.playAlbum(album) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💿", fontSize = 20.sp)
                        Spacer(Modifier = Modifier.width(12.dp))
                        Column {
                            Text(album, color = Color.White, fontWeight = FontWeight.Medium)
                            Text("$count songs", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
            if (playlists.isEmpty() && albums.isEmpty()) {
                item {
                    Text(
                        "Create a playlist or scan your library to see albums.",
                        color = Color.Gray,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        }
    }
}
