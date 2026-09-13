package com.pulseplayer.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pulseplayer.music.viewmodel.PlaybackViewModel

@Composable
fun DownloadsScreen(viewModel: PlaybackViewModel) {
    val songs by viewModel.songs.collectAsState()
    val downloaded = remember(songs) { songs.filter { it.isDownloaded || it.sourceType == "stream" } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Download, null, tint = Color(0xFF7C4DFF))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Downloads", color = Color.White, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (downloaded.isEmpty()) {
            Text("No downloaded or streamed tracks yet.", color = Color.Gray)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(downloaded, key = { it.id }) { song ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.playSong(downloaded, song) }
                    ) {
                        ListItem(
                            headlineContent = { Text(song.title, color = Color.White, maxLines = 1) },
                            supportingContent = { Text(song.artist, color = Color.Gray, maxLines = 1) },
                            leadingContent = {
                                AsyncImage(
                                    model = song.coverUrl.ifBlank { null },
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp)
                                )
                            },
                            trailingContent = {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4ade80))
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}
