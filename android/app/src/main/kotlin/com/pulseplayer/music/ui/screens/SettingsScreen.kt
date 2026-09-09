package com.pulseplayer.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    spotifyLoggedIn: Boolean = false,
    youtubeLoggedIn: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1a1a2e))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Download Sources Section
        SettingsSection(title = "Download Sources") {
            // Spotify Login
            SettingsItem(
                title = "Spotify Login",
                subtitle = if (spotifyLoggedIn) "Logged in" else "Not logged in",
                statusColor = if (spotifyLoggedIn) Color(0xFF4ade80) else Color(0xFFef4444),
                onClick = {
                    navController.navigate("webLogin?platform=spotify")
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

            // YouTube Music Login
            SettingsItem(
                title = "YouTube Music Login",
                subtitle = if (youtubeLoggedIn) "Logged in" else "Not logged in",
                statusColor = if (youtubeLoggedIn) Color(0xFF4ade80) else Color(0xFFef4444),
                onClick = {
                    navController.navigate("webLogin?platform=youtube")
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // General Settings Section
        SettingsSection(title = "General") {
            SettingsItem(
                title = "Audio Quality",
                subtitle = "High (320kbps)",
                onClick = { /* Navigate to quality settings */ }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

            SettingsItem(
                title = "Download Location",
                subtitle = "/storage/emulated/0/Music/PulsePlayer",
                onClick = { /* Navigate to storage settings */ }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

            SettingsItem(
                title = "Clear Cache",
                subtitle = "Free up space",
                onClick = { /* Clear cache logic */ }
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    statusColor: Color? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontSize = 16.sp
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        }

        statusColor?.let { color ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
        }

        Icon(
            painter = androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}
