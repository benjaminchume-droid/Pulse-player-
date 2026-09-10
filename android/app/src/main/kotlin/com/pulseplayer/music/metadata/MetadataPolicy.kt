package com.pulseplayer.music.metadata

import com.pulseplayer.music.data.AskBeforeChanging
import com.pulseplayer.music.data.ConfidenceFilter
import com.pulseplayer.music.data.ConfidenceLevel
import com.pulseplayer.music.data.MetadataSettings

class MetadataPolicy(private val settings: MetadataSettings) {

    fun determineChanges(
        existingMetadata: ExistingMetadata,
        newMetadata: CanonicalTrackMetadata,
        isAmbiguous: Boolean = false
    ): MetadataChangeSet {
        val changes = mutableListOf<MetadataFieldChange>()
        val shouldReplace = settings.replaceExistingMetadata

        if (shouldFillField(MetadataField.TITLE, existingMetadata.title, shouldReplace) &&
            MetadataEnricher.isValidTitle(newMetadata.title)
        ) {
            changes.add(MetadataFieldChange(MetadataField.TITLE, existingMetadata.title, newMetadata.title))
        }
        if (shouldFillField(MetadataField.ARTIST, existingMetadata.artist, shouldReplace)) {
            changes.add(MetadataFieldChange(MetadataField.ARTIST, existingMetadata.artist, newMetadata.artist))
        }
        if (shouldFillField(MetadataField.ALBUM, existingMetadata.album, shouldReplace)) {
            changes.add(MetadataFieldChange(MetadataField.ALBUM, existingMetadata.album, newMetadata.album))
        }
        if (settings.fillYear && shouldFillField(MetadataField.YEAR, existingMetadata.year?.toString(), shouldReplace)) {
            changes.add(
                MetadataFieldChange(
                    MetadataField.YEAR,
                    existingMetadata.year?.toString(),
                    newMetadata.year?.toString()
                )
            )
        }

        val shouldAddArtwork = settings.fillArtwork && (shouldReplace || !existingMetadata.hasArtwork)

        return MetadataChangeSet(
            changes = changes,
            shouldAddArtwork = shouldAddArtwork,
            requiresConfirmation = when (settings.askBeforeChanging) {
                AskBeforeChanging.ALWAYS -> true
                AskBeforeChanging.AMBIGUOUS_ONLY -> isAmbiguous
                AskBeforeChanging.NEVER -> false
            },
            isAmbiguous = isAmbiguous
        )
    }

    private fun shouldFillField(field: MetadataField, existingValue: String?, shouldReplace: Boolean): Boolean {
        val isMissing = existingValue.isNullOrBlank() ||
            (field == MetadataField.TITLE && !MetadataEnricher.isValidTitle(existingValue))
        return when (field) {
            MetadataField.TITLE -> settings.fillMissingMetadata && (isMissing || shouldReplace)
            MetadataField.ARTIST -> settings.fillArtist && (isMissing || shouldReplace)
            MetadataField.ALBUM -> settings.fillAlbum && (isMissing || shouldReplace)
            MetadataField.YEAR -> settings.fillYear && (isMissing || shouldReplace)
        }
    }

    fun meetsConfidenceThreshold(confidence: ConfidenceLevel): Boolean {
        return when (settings.confidenceFilter) {
            ConfidenceFilter.HIGH_ONLY -> confidence == ConfidenceLevel.HIGH
            ConfidenceFilter.HIGH_AND_MEDIUM ->
                confidence == ConfidenceLevel.HIGH || confidence == ConfidenceLevel.MEDIUM
        }
    }
}

enum class MetadataField { TITLE, ARTIST, ALBUM, YEAR }

data class ExistingMetadata(
    val title: String?,
    val artist: String?,
    val album: String?,
    val year: Int?,
    val hasArtwork: Boolean
)

data class MetadataChangeSet(
    val changes: List<MetadataFieldChange>,
    val shouldAddArtwork: Boolean,
    val requiresConfirmation: Boolean,
    val isAmbiguous: Boolean
)

data class MetadataFieldChange(
    val field: MetadataField,
    val oldValue: String?,
    val newValue: String?
)
