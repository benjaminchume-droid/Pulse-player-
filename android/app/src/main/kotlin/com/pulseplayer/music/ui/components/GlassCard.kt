package com.pulseplayer.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pulseplayer.music.ui.theme.RealmManager

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val theme by RealmManager.currentTheme.collectAsState()
    val isAmoled by RealmManager.amoledMode.collectAsState()

    // Base background colors based on whether theme is light, dark, or in amoled mode
    val cardBg = when {
        isAmoled -> Color(0xFF070707)
        theme.isLight -> Color(0xD0FFFFFF)
        theme.id == "overclocked_cyber" -> Color(0xE1050D14)
        theme.id == "dark_void" -> Color(0x3312061D)
        else -> Color(0x19FFFFFF) // Soft glass
    }

    val borderTint = when {
        isAmoled -> Color(0x1AFFFFFF)
        theme.isLight -> Color(0x334A5568)
        else -> theme.accentColor.copy(alpha = 0.18f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(cardBg)
            .border(1.dp, borderTint, RoundedCornerShape(cornerRadius))
            .padding(16.dp),
        content = content
    )
}
