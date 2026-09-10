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
import com.pulseplayer.music.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataSettingsScreen(
    settings: MetadataSettings,
    onSettingsChange: (MetadataSettings) -> Unit,
    onApply: () -> Unit
) {
    var localSettings by remember { mutableStateOf(settings) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1a1a2e))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Automatic Metadata Filling",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Main toggle
        SettingsSection {
            SettingSwitch(
                title = "Automatic Metadata Filling",
                subtitle = "Automatically identify music and fill missing metadata",
                checked = localSettings.automaticMetadataFillingEnabled,
                onCheckedChange = { enabled ->
                    localSettings = localSettings.copy(automaticMetadataFillingEnabled = enabled)
                    onSettingsChange(localSettings)
                }
            )
        }

        if (localSettings.automaticMetadataFillingEnabled) {
            Spacer(modifier = Modifier.height(16.dp))

            // Recognition mode
            SettingsSection(title = "Recognition") {
                SettingRadio(
                    title = "Only when playing",
                    subtitle = "Identify songs as you play them",
                    selected = localSettings.recognitionMode == RecognitionMode.ONLY_WHEN_PLAYING,
                    onClick = {
                        localSettings = localSettings.copy(recognitionMode = RecognitionMode.ONLY_WHEN_PLAYING)
                        onSettingsChange(localSettings)
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                SettingRadio(
                    title = "Scan library automatically",
                    subtitle = "Scan your entire library in the background",
                    selected = localSettings.recognitionMode == RecognitionMode.SCAN_LIBRARY_AUTOMATICALLY,
                    onClick = {
                        localSettings = localSettings.copy(recognitionMode = RecognitionMode.SCAN_LIBRARY_AUTOMATICALLY)
                        onSettingsChange(localSettings)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recognition source
            SettingsSection(title = "Recognition source") {
                SettingRadio(
                    title = "Use audio from this app",
                    subtitle = "Analyze audio directly from playback",
                    selected = localSettings.recognitionSource == RecognitionSource.AUDIO_FROM_APP,
                    onClick = {
                        localSettings = localSettings.copy(recognitionSource = RecognitionSource.AUDIO_FROM_APP)
                        onSettingsChange(localSettings)
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                SettingRadio(
                    title = "Use microphone when playing",
                    subtitle = "Use device microphone (requires permission)",
                    selected = localSettings.recognitionSource == RecognitionSource.MICROPHONE_WHEN_PLAYING,
                    onClick = {
                        localSettings = localSettings.copy(recognitionSource = RecognitionSource.MICROPHONE_WHEN_PLAYING)
                        onSettingsChange(localSettings)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // What to fill
            SettingsSection(title = "Apply") {
                SettingCheckbox(
                    title = "Missing metadata",
                    checked = localSettings.fillMissingMetadata,
                    onCheckedChange = { checked ->
                        localSettings = localSettings.copy(fillMissingMetadata = checked)
                        onSettingsChange(localSettings)
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                SettingCheckbox(
                    title = "Album artwork",
                    checked = localSettings.fillArtwork,
                    onCheckedChange = { checked ->
                        localSettings = localSettings.copy(fillArtwork = checked)
                        onSettingsChange(localSettings)
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                SettingCheckbox(
                    title = "Artist",
                    checked = localSettings.fillArtist,
                    onCheckedChange = { checked ->
                        localSettings = localSettings.copy(fillArtist = checked)
                        onSettingsChange(localSettings)
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                SettingCheckbox(
                    title = "Album",
                    checked = localSettings.fillAlbum,
                    onCheckedChange = { checked ->
                        localSettings = localSettings.copy(fillAlbum = checked)
                        onSettingsChange(localSettings)
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                SettingCheckbox(
                    title = "Year",
                    checked = localSettings.fillYear,
                    onCheckedChange = { checked ->
                        localSettings = localSettings.copy(fillYear = checked)
                        onSettingsChange(localSettings)
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                SettingCheckbox(
                    title = "Replace existing metadata",
                    subtitle = "Overwrite existing metadata (not recommended)",
                    checked = localSettings.replaceExistingMetadata,
                    onCheckedChange = { checked ->
                        localSettings = localSettings.copy(replaceExistingMetadata = checked)
                        onSettingsChange(localSettings)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confidence
            SettingsSection(title = "Confidence") {
                SettingRadio(
                    title = "High only",
                    subtitle = "Only apply high-confidence matches",
                    selected = localSettings.confidenceFilter == ConfidenceFilter.HIGH_ONLY,
                    onClick = {
                        localSettings = localSettings.copy(confidenceFilter = ConfidenceFilter.HIGH_ONLY)
                        onSettingsChange(localSettings)
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                SettingRadio(
                    title = "High + medium",
                    subtitle = "Apply high and medium confidence matches",
                    selected = localSettings.confidenceFilter == ConfidenceFilter.HIGH_AND_MEDIUM,
                    onClick = {
                        localSettings = localSettings.copy(confidenceFilter = ConfidenceFilter.HIGH_AND_MEDIUM)
                        onSettingsChange(localSettings)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ask before changing
            SettingsSection(title = "Ask before changing") {
                SettingRadio(
                    title = "Ambiguous matches only",
                    selected = localSettings.askBeforeChanging == AskBeforeChanging.AMBIGUOUS_ONLY,
                    onClick = {
                        localSettings = localSettings.copy(askBeforeChanging = AskBeforeChanging.AMBIGUOUS_ONLY)
                        onSettingsChange(localSettings)
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                SettingRadio(
                    title = "Always",
                    selected = localSettings.askBeforeChanging == AskBeforeChanging.ALWAYS,
                    onClick = {
                        localSettings = localSettings.copy(askBeforeChanging = AskBeforeChanging.ALWAYS)
                        onSettingsChange(localSettings)
                    }
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)

                SettingRadio(
                    title = "Never",
                    selected = localSettings.askBeforeChanging == AskBeforeChanging.NEVER,
                    onClick = {
                        localSettings = localSettings.copy(askBeforeChanging = AskBeforeChanging.NEVER)
                        onSettingsChange(localSettings)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Apply button
        Button(
            onClick = onApply,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4ade80)
            )
        ) {
            Text(
                text = "Apply Changes",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(16.dp)
    ) {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        content()
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
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

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF4ade80),
                checkedTrackColor = Color(0xFF4ade80).copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun SettingRadio(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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

        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF4ade80)
            )
        )
    }
}

@Composable
private fun SettingCheckbox(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
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

        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF4ade80)
            )
        )
    }
}
