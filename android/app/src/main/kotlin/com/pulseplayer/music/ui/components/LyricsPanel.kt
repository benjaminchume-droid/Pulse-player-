package com.pulseplayer.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulseplayer.music.data.Song
import com.pulseplayer.music.lyrics.LyricsRepository
import com.pulseplayer.music.ui.theme.RealmManager
import kotlinx.coroutines.launch

@Composable
fun LyricsPanel(
    song: Song?,
    positionMs: Long,
    isLoading: Boolean,
    onFetchLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme by RealmManager.currentTheme.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val parsed = remember(song?.id, song?.syncedLyrics, song?.lyrics) {
        when {
            song == null -> emptyList()
            song.syncedLyrics.isNotBlank() -> LyricsRepository.parseLrc(song.syncedLyrics)
            song.lyrics.isNotBlank() -> song.lyrics.lines().filter { it.isNotBlank() }.mapIndexed { i, line ->
                (i * 3000L) to line // fake timing for plain lyrics scroll
            }
            else -> emptyList()
        }
    }

    val activeIdx = remember(parsed, positionMs) {
        LyricsRepository.activeLineIndex(parsed, positionMs)
    }

    LaunchedEffect(activeIdx) {
        if (activeIdx >= 0 && parsed.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(activeIdx.coerceAtMost(parsed.lastIndex))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp, max = 280.dp)
            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "LYRICS",
                color = theme.accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            if (song != null && song.lyrics.isBlank() && song.syncedLyrics.isBlank()) {
                TextButton(onClick = onFetchLyrics, enabled = !isLoading) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = theme.accentColor, strokeWidth = 2.dp)
                    } else {
                        Text("Fetch", color = theme.accentColor, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading -> {
                Box(Modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = theme.accentColor)
                }
            }
            parsed.isEmpty() -> {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No lyrics yet — tap Fetch",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(parsed) { index, pair ->
                        val isActive = index == activeIdx
                        Text(
                            text = pair.second,
                            color = if (isActive) theme.accentColor else Color.White.copy(alpha = 0.45f),
                            fontSize = if (isActive) 16.sp else 13.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
