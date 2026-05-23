package com.pulseplayer.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.pulseplayer.music.ui.theme.BackgroundDark

@Composable
fun AnimatedGlowBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "BkgGlowTransition")

    // Infinite float position offset animations
    val offset1X by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, ease = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow1X"
    )
    val offset1Y by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, ease = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow1Y"
    )

    val offset2X by infiniteTransition.animateFloat(
        initialValue = 800f,
        targetValue = 150f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, ease = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow2X"
    )
    val offset2Y by infiniteTransition.animateFloat(
        initialValue = 200f,
        targetValue = 1100f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, ease = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow2Y"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw floating cyan glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1F00F0FF), Color.Transparent),
                    center = Offset(offset1X, offset1Y),
                    radius = 450f
                ),
                radius = 450f,
                center = Offset(offset1X, offset1Y)
            )

            // Draw floating purple glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1AD600FF), Color.Transparent),
                    center = Offset(offset2X, offset2Y),
                    radius = 450f
                ),
                radius = 450f,
                center = Offset(offset2X, offset2Y)
            )
        }
    }
}
