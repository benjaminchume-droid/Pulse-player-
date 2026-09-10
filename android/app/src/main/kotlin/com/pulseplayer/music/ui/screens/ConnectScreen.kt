package com.pulseplayer.music.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulseplayer.music.ui.components.GlassCard
import com.pulseplayer.music.ui.theme.RealmManager
import com.pulseplayer.music.viewmodel.PlaybackViewModel
import java.util.UUID

@Composable
fun ConnectScreen(viewModel: PlaybackViewModel, modifier: Modifier = Modifier) {
    val theme by RealmManager.currentTheme.collectAsState()
    var roomCode by remember { mutableStateOf("") }
    var myRoom by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Idle — create or join a room") }
    var host by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Listen Together", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light)
        Text(
            "Share a room code. Enter your own WebSocket relay URL if you have one.",
            color = Color.Gray,
            fontSize = 12.sp
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Relay", color = theme.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("WebSocket relay URL (optional)") },
                    placeholder = { Text("wss://…") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(status, color = Color.LightGray, fontSize = 12.sp)
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Host a room", color = Color.White, fontWeight = FontWeight.SemiBold)
                Button(
                    onClick = {
                        myRoom = UUID.randomUUID().toString().take(6).uppercase()
                        status = "Hosting room $myRoom. Share this code."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Create room code") }
                if (myRoom != null) {
                    Text(
                        "Code: $myRoom",
                        color = theme.accentColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Join a room", color = Color.White, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = roomCode,
                    onValueChange = { roomCode = it.uppercase().take(8) },
                    label = { Text("Room code") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = {
                        status = if (roomCode.length >= 4) "Joined $roomCode"
                        else "Enter a valid room code"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Join") }
            }
        }
    }
}
