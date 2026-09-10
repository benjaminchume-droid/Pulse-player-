package com.pulseplayer.music.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Placeholder — settings live on SettingsScreen for now. */
@Composable
fun MetadataSettingsScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Automatic Metadata Filling", color = Color.White, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Configure enrich-all and recognition from Settings. " +
                "Recognition runs once per song while playing.",
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}
