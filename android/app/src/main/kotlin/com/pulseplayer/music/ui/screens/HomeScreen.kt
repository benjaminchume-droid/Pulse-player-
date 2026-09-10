package com.pulseplayer.music.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulseplayer.music.viewmodel.PlaybackViewModel

@Composable
fun HomeScreen(viewModel: PlaybackViewModel) {
    val songs by viewModel.songs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val level by viewModel.userLevel.collectAsState()
    val xp by viewModel.userXp.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Home", color = Color.White, fontSize = 24.sp, style = MaterialTheme.typography.titleLarge)
            Text("Level $level · $xp XP", color = Color.Gray, fontSize = 13.sp)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatChip(Icons.Default.LibraryMusic, "${songs.size}", "Songs")
                StatChip(Icons.Default.QueueMusic, "${playlists.size}", "Playlists")
            }
        }
        item {
            Button(
                onClick = { viewModel.scanDeviceAudio() },
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isScanning) "Scanning…" else "Scan library")
            }
        }
        item {
            Text("Recently in library", color = Color.White, fontSize = 16.sp)
        }
        items(songs.take(20), key = { it.id }) { song ->
            ListItem(
                headlineContent = { Text(song.title, color = Color.White, maxLines = 1) },
                supportingContent = { Text(song.artist, color = Color.Gray, maxLines = 1) },
                modifier = Modifier.fillMaxWidth(),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Surface(color = Color(0x22FFFFFF), shape = MaterialTheme.shapes.medium) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color(0xFFB39DDB))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(value, color = Color.White, fontSize = 16.sp)
                Text(label, color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}
