package com.pulseplayer.music.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulseplayer.music.ui.components.UpdateSection
import com.pulseplayer.music.viewmodel.PlaybackViewModel

@Composable
fun SettingsScreen(viewModel: PlaybackViewModel) {
    val crossfade by viewModel.crossfadeSeconds.collectAsState()
    val isEnriching by viewModel.isEnriching.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", color = Color.White, fontSize = 22.sp)

        Text("Playback", color = Color(0xFFB39DDB), fontSize = 14.sp)
        Text("Crossfade: ${crossfade}s", color = Color.White)
        Slider(
            value = crossfade.toFloat(),
            onValueChange = { viewModel.setCrossfadeSeconds(it.toInt()) },
            valueRange = 0f..25f,
            steps = 4
        )

        Divider(color = Color(0x33FFFFFF))

        Text("Metadata", color = Color(0xFFB39DDB), fontSize = 14.sp)
        Text(
            "Identify while playing uses AudD (or ShazamKit when configured). " +
                "Each track is recognized at most once.",
            color = Color.Gray,
            fontSize = 12.sp
        )
        Button(
            onClick = { viewModel.enrichAllMetadata() },
            enabled = !isEnriching,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
        ) {
            Text(if (isEnriching) "Enriching…" else "Enrich all library metadata")
        }

        Divider(color = Color(0x33FFFFFF))

        Text("Updates", color = Color(0xFFB39DDB), fontSize = 14.sp)
        UpdateSection()

        Divider(color = Color(0x33FFFFFF))
        Text("Pulse Player · Velocity Lab / Glass Line", color = Color.Gray, fontSize = 12.sp)
    }
}
