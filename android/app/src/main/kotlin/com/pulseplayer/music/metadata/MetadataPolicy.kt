package com.pulseplayer.music.metadata

import com.pulseplayer.music.data.*

/**
 * Policy engine for metadata operations.
 */
class MetadataPolicy(private val settings: MetadataSettings) {

    /**
     * Determine what metadata changes to apply based on user settings.
     */
    fun determineChanges(
        existingMetadata: ExistingMetadata,
        newMetadata: CanonicalTrackMetadata
    ): MetadataChangeSet {
        val changes = mutableListOf<MetadataFieldChange>()

        // Check if we should replace existing metadata
        val shouldReplace = settings.replaceExistingMetadata

        // Title
        if (shouldFillField(MetadataField.TITLE, existingMetadata.title, shouldReplace)) {
            changes.add(MetadataFieldChange(MetadataField.TITLE, existingMetadata.title, newMetadata.title))
        }

        // Artist
        if (shouldFillField(MetadataField.ARTIST, existingMetadata.artist, shouldReplace)) {
            changes.add(MetadataFieldChange(MetadataField.ARTIST, existingMetadata.artist, newMetadata.artist))
        }

        // Album
        if (shouldFillField(MetadataField.ALBUM, existingMetadata.album, shouldReplace)) {
            changes.add(MetadataFieldChange(MetadataField.ALBUM, existingMetadata.album, newMetadata.album))
        }

        // Year
        if (settings.fillYear && shouldFillField(MetadataField.YEAR, existingMetadata.year?.toString(), shouldReplace)) {
            changes.add(MetadataFieldChange(MetadataField.YEAR, existingMetadata.year?.toString(), newMetadata.year?.toString()))
        }

        // Artwork
        val shouldAddArtwork = settings.fillArtwork && 
            (shouldReplace || existingMetadata.hasArtwork.not())

        return MetadataChangeSet(
            changes = changes,
            shouldAddArtwork = shouldAddArtwork,
            requiresConfirmation = shouldRequireConfirmation(newMetadata),
            isAmbiguous = newMetadata.isAmbiguous
        )
    }

    private fun shouldFillField(field: MetadataField, existingValue: String?, shouldReplace: Boolean): Boolean {
        val isMissing = existingValue.isNullOrBlank()

        return when (field) {
            MetadataField.TITLE -> settings.fillMissingMetadata && (isMissing || shouldReplace)
            MetadataField.ARTIST -> settings.fillArtist && (isMissing || shouldReplace)
            MetadataField.ALBUM -> settings.fillAlbum && (isMissing || shouldReplace)
            MetadataField.YEAR -> settings.fillYear && (isMissing || shouldReplace)
        }
    }

    private fun shouldRequireConfirmation(metadata: CanonicalTrackMetadata): Boolean {
        return when (settings.askBeforeChanging) {
            AskBeforeChanging.ALWAYS -> true
            AskBeforeChanging.AMBIGUOUS_ONLY -> metadata.isAmbiguous
            AskBeforeChanging.NEVER -> false
        }
    }

    /**
     * Check if confidence meets the filter threshold.
     */
    fun meetsConfidenceThreshold(confidence: ConfidenceLevel): Boolean {
        return when (settings.confidenceFilter) {
            ConfidenceFilter.HIGH_ONLY -> confidence == ConfidenceLevel.HIGH
            ConfidenceFilter.HIGH_AND_MEDIUM -> 
                confidence == ConfidenceLevel.HIGH || confidence == ConfidenceLevel.MEDIUM
        }
    }
}

enum class MetadataField {
    TITLE,
    ARTIST,
    ALBUM,
    YEAR
}

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
