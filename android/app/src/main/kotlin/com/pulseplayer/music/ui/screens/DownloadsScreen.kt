package com.pulseplayer.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulseplayer.music.ui.theme.RealmManager
import com.pulseplayer.music.viewmodel.PlaybackViewModel

@Composable
fun DownloadsScreen(viewModel: PlaybackViewModel, modifier: Modifier = Modifier) {
    val songs by viewModel.songs.collectAsState()
    val theme by RealmManager.currentTheme.collectAsState()
    val downloaded = remember(songs) {
        songs.filter { it.isDownloaded || it.sourceType == "download" || it.sourceType == "stream" }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Downloads & Streams", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light)
        Text(
            "Saved online tracks and direct downloads. Legal sources only.",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        if (downloaded.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No downloaded or streamed tracks yet.\nSearch Online from the top search icon.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                items(downloaded) { song ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x0AFFFFFF))
                            .clickable { viewModel.playSong(downloaded, song) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(song.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                            Text("${song.artist} · ${song.sourceType}", color = theme.accentColor, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
