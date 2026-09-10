package com.pulseplayer.music.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.Date

/**
 * Metadata scan job representing a library scanning operation.
 */
@Entity(tableName = "metadata_scan_jobs")
data class MetadataScanJob(
    @PrimaryKey val id: String,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val status: ScanJobStatus,
    val totalItems: Int = 0,
    val processedItems: Int = 0,
    val identifiedCount: Int = 0,
    val updatedCount: Int = 0,
    val noMatchCount: Int = 0,
    val ambiguousCount: Int = 0,
    val failedCount: Int = 0,
    val isPaused: Boolean = false
)

enum class ScanJobStatus {
    QUEUED,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Individual item in a metadata scan job.
 */
@Entity(tableName = "metadata_scan_items")
data class MetadataScanItem(
    @PrimaryKey val id: String,
    val jobId: String,
    val filePath: String,
    val fileUri: String,
    val fileSize: Long,
    val lastModified: Long,
    val status: ScanItemStatus,
    val matchId: String? = null,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null
)

enum class ScanItemStatus {
    QUEUED,
    PROCESSING,
    MATCHED,
    UPDATED,
    NO_MATCH,
    AMBIGUOUS,
    FAILED,
    SKIPPED,
    CANCELLED
}

/**
 * Recognition match result from ShazamKit or other provider.
 */
@Entity(tableName = "metadata_matches")
data class MetadataMatch(
    @PrimaryKey val id: String,
    val scanItemId: String,
    val provider: String,
    val trackTitle: String,
    val artist: String,
    val album: String?,
    val albumArtist: String?,
    val year: Int?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val genre: String?,
    val composer: String?,
    val isrc: String?,
    val artworkUrl: String?,
    val duration: Long?,
    val confidence: ConfidenceLevel,
    val isAmbiguous: Boolean,
    val rawMetadata: String,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW,
    NO_MATCH,
    AMBIGUOUS
}

/**
 * Metadata change record for history and rollback.
 */
@Entity(tableName = "metadata_changes")
data class MetadataChange(
    @PrimaryKey val id: String,
    val songId: String,
    val filePath: String,
    val changeType: ChangeType,
    val field: String,
    val oldValue: String?,
    val newValue: String?,
    val source: String,
    val matchId: String?,
    val applied: Boolean,
    val canRollback: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val rolledbackAt: Long? = null
)

enum class ChangeType {
    CREATE,
    UPDATE,
    DELETE,
    ROLLBACK
}

/**
 * User settings for automatic metadata filling.
 */
@Entity(tableName = "metadata_settings")
data class MetadataSettings(
    @PrimaryKey val id: String = "default",
    val automaticMetadataFillingEnabled: Boolean = false,
    val recognitionMode: RecognitionMode = RecognitionMode.ONLY_WHEN_PLAYING,
    val recognitionSource: RecognitionSource = RecognitionSource.AUDIO_FROM_APP,
    val fillMissingMetadata: Boolean = true,
    val fillArtwork: Boolean = true,
    val fillArtist: Boolean = true,
    val fillAlbum: Boolean = true,
    val fillYear: Boolean = true,
    val replaceExistingMetadata: Boolean = false,
    val confidenceFilter: ConfidenceFilter = ConfidenceFilter.HIGH_ONLY,
    val askBeforeChanging: AskBeforeChanging = AskBeforeChanging.AMBIGUOUS_ONLY,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

enum class RecognitionMode {
    ONLY_WHEN_PLAYING,
    SCAN_LIBRARY_AUTOMATICALLY
}

enum class RecognitionSource {
    AUDIO_FROM_APP,
    MICROPHONE_WHEN_PLAYING
}

enum class ConfidenceFilter {
    HIGH_ONLY,
    HIGH_AND_MEDIUM
}

enum class AskBeforeChanging {
    AMBIGUOUS_ONLY,
    ALWAYS,
    NEVER
}

/**
 * Cached artwork for identified recordings.
 */
@Entity(tableName = "artwork_cache")
data class ArtworkCache(
    @PrimaryKey val id: String,
    val recordingId: String,
    val artworkUrl: String,
    val localPath: String,
    val fileSize: Long,
    val width: Int,
    val height: Int,
    val format: String,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null
)
