package com.pulseplayer.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulseplayer.music.viewmodel.PlaybackViewModel

@Composable
fun PlaylistsScreen(viewModel: PlaybackViewModel) {
    val playlists by viewModel.playlists.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Playlists", color = Color.White, fontSize = 22.sp)
            IconButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, "Create", tint = Color(0xFF7C4DFF))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (playlists.isEmpty()) {
            Text("No playlists yet. Tap + to create one.", color = Color.Gray)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(playlists, key = { it.id }) { pl ->
                    ListItem(
                        headlineContent = { Text(pl.name, color = Color.White) },
                        supportingContent = {
                            Text(pl.description.ifBlank { "Playlist" }, color = Color.Gray, maxLines = 1)
                        },
                        leadingContent = {
                            Icon(Icons.Default.QueueMusic, null, tint = Color(0xFFB39DDB))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.playPlaylist(pl) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New playlist") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.createPlaylist(name.trim())
                            name = ""
                            showCreate = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Cancel") }
            }
        )
    }
}
