package com.pulseplayer.music.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Song::class,
        Playlist::class,
        MetadataScanJob::class,
        MetadataScanItem::class,
        MetadataMatch::class,
        MetadataChange::class,
        MetadataSettings::class,
        ArtworkCache::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao
    abstract fun metadataDao(): MetadataDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        fun getDatabase(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "pulse_player_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create metadata tables
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS metadata_scan_jobs (
                        id TEXT PRIMARY KEY NOT NULL,
                        createdAt INTEGER NOT NULL,
                        startedAt INTEGER,
                        completedAt INTEGER,
                        status TEXT NOT NULL,
                        totalItems INTEGER NOT NULL,
                        processedItems INTEGER NOT NULL,
                        identifiedCount INTEGER NOT NULL,
                        updatedCount INTEGER NOT NULL,
                        noMatchCount INTEGER NOT NULL,
                        ambiguousCount INTEGER NOT NULL,
                        failedCount INTEGER NOT NULL,
                        isPaused INTEGER NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS metadata_scan_items (
                        id TEXT PRIMARY KEY NOT NULL,
                        jobId TEXT NOT NULL,
                        filePath TEXT NOT NULL,
                        fileUri TEXT NOT NULL,
                        fileSize INTEGER NOT NULL,
                        lastModified INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        matchId TEXT,
                        retryCount INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        processedAt INTEGER
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS metadata_matches (
                        id TEXT PRIMARY KEY NOT NULL,
                        scanItemId TEXT NOT NULL,
                        provider TEXT NOT NULL,
                        trackTitle TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        album TEXT,
                        albumArtist TEXT,
                        year INTEGER,
                        trackNumber INTEGER,
                        discNumber INTEGER,
                        genre TEXT,
                        composer TEXT,
                        isrc TEXT,
                        artworkUrl TEXT,
                        duration INTEGER,
                        confidence TEXT NOT NULL,
                        isAmbiguous INTEGER NOT NULL,
                        rawMetadata TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS metadata_changes (
                        id TEXT PRIMARY KEY NOT NULL,
                        songId TEXT NOT NULL,
                        filePath TEXT NOT NULL,
                        changeType TEXT NOT NULL,
                        field TEXT NOT NULL,
                        oldValue TEXT,
                        newValue TEXT,
                        source TEXT NOT NULL,
                        matchId TEXT,
                        applied INTEGER NOT NULL,
                        canRollback INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        rolledbackAt INTEGER
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS metadata_settings (
                        id TEXT PRIMARY KEY NOT NULL,
                        automaticMetadataFillingEnabled INTEGER NOT NULL,
                        recognitionMode TEXT NOT NULL,
                        recognitionSource TEXT NOT NULL,
                        fillMissingMetadata INTEGER NOT NULL,
                        fillArtwork INTEGER NOT NULL,
                        fillArtist INTEGER NOT NULL,
                        fillAlbum INTEGER NOT NULL,
                        fillYear INTEGER NOT NULL,
                        replaceExistingMetadata INTEGER NOT NULL,
                        confidenceFilter TEXT NOT NULL,
                        askBeforeChanging TEXT NOT NULL,
                        lastUpdatedAt INTEGER NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS artwork_cache (
                        id TEXT PRIMARY KEY NOT NULL,
                        recordingId TEXT NOT NULL,
                        artworkUrl TEXT NOT NULL,
                        localPath TEXT NOT NULL,
                        fileSize INTEGER NOT NULL,
                        width INTEGER NOT NULL,
                        height INTEGER NOT NULL,
                        format TEXT NOT NULL,
                        lastAccessedAt INTEGER NOT NULL,
                        expiresAt INTEGER
                    )
                """)

                // Insert default settings
                db.execSQL("""
                    INSERT OR IGNORE INTO metadata_settings 
                    (id, automaticMetadataFillingEnabled, recognitionMode, recognitionSource, 
                     fillMissingMetadata, fillArtwork, fillArtist, fillAlbum, fillYear, 
                     replaceExistingMetadata, confidenceFilter, askBeforeChanging, lastUpdatedAt)
                    VALUES ('default', 0, 'ONLY_WHEN_PLAYING', 'AUDIO_FROM_APP', 
                            1, 1, 1, 1, 1, 0, 'HIGH_ONLY', 'AMBIGUOUS_ONLY', 
                            ${System.currentTimeMillis()})
                """)
            }
        }
    }
}

/**
 * Type converters for enum and date types.
 */
class Converters {
    @TypeConverter
    fun fromScanJobStatus(status: ScanJobStatus): String = status.name

    @TypeConverter
    fun toScanJobStatus(status: String): ScanJobStatus = ScanJobStatus.valueOf(status)

    @TypeConverter
    fun fromScanItemStatus(status: ScanItemStatus): String = status.name

    @TypeConverter
    fun toScanItemStatus(status: String): ScanItemStatus = ScanItemStatus.valueOf(status)

    @TypeConverter
    fun fromConfidenceLevel(confidence: ConfidenceLevel): String = confidence.name

    @TypeConverter
    fun toConfidenceLevel(confidence: String): ConfidenceLevel = ConfidenceLevel.valueOf(confidence)

    @TypeConverter
    fun fromChangeType(changeType: ChangeType): String = changeType.name

    @TypeConverter
    fun toChangeType(changeType: String): ChangeType = ChangeType.valueOf(changeType)

    @TypeConverter
    fun fromRecognitionMode(mode: RecognitionMode): String = mode.name

    @TypeConverter
    fun toRecognitionMode(mode: String): RecognitionMode = RecognitionMode.valueOf(mode)

    @TypeConverter
    fun fromRecognitionSource(source: RecognitionSource): String = source.name

    @TypeConverter
    fun toRecognitionSource(source: String): RecognitionSource = RecognitionSource.valueOf(source)

    @TypeConverter
    fun fromConfidenceFilter(filter: ConfidenceFilter): String = filter.name

    @TypeConverter
    fun toConfidenceFilter(filter: String): ConfidenceFilter = ConfidenceFilter.valueOf(filter)

    @TypeConverter
    fun fromAskBeforeChanging(ask: AskBeforeChanging): String = ask.name

    @TypeConverter
    fun toAskBeforeChanging(ask: String): AskBeforeChanging = AskBeforeChanging.valueOf(ask)
}
