package com.pulseplayer.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDownloads: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0f0f1a))
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> HomeTabContent()
                1 -> MixesTabContent()
                2 -> LibraryTabContent()
            }
        }

        NavigationBar(
            containerColor = Color(0xFF1a1a2e),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
        ) {
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = androidx.compose.material.icons.Icons.Filled.Home,
                        contentDescription = "Home",
                        tint = if (selectedTab == 0) Color(0xFF4ade80) else Color.White.copy(alpha = 0.5f)
                    )
                },
                label = { Text("Home", fontSize = 11.sp) },
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.White.copy(alpha = 0.1f)
                )
            )
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = androidx.compose.material.icons.Icons.Filled.QueueMusic,
                        contentDescription = "Mixes",
                        tint = if (selectedTab == 1) Color(0xFF4ade80) else Color.White.copy(alpha = 0.5f)
                    )
                },
                label = { Text("Mixes", fontSize = 11.sp) },
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.White.copy(alpha = 0.1f)
                )
            )
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = androidx.compose.material.icons.Icons.Filled.AudioStream,
                        contentDescription = "Streaming",
                        tint = if (selectedTab == 2) Color(0xFF4ade80) else Color.White.copy(alpha = 0.5f)
                    )
                },
                label = { Text("Stream", fontSize = 11.sp) },
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.White.copy(alpha = 0.1f)
                )
            )
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = androidx.compose.material.icons.Icons.Filled.LibraryMusic,
                        contentDescription = "Library",
                        tint = if (selectedTab == 3) Color(0xFF4ade80) else Color.White.copy(alpha = 0.5f)
                    )
                },
                label = { Text("Library", fontSize = 11.sp) },
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.White.copy(alpha = 0.1f)
                )
            )
        }
    }
}

@Composable
private fun HomeTabContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Home",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MixesTabContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Mixes",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LibraryTabContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Library",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
