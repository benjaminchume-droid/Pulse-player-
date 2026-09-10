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
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class, SongIdListConverter::class)
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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter fun fromScanJobStatus(status: ScanJobStatus): String = status.name
    @TypeConverter fun toScanJobStatus(status: String): ScanJobStatus = ScanJobStatus.valueOf(status)
    @TypeConverter fun fromScanItemStatus(status: ScanItemStatus): String = status.name
    @TypeConverter fun toScanItemStatus(status: String): ScanItemStatus = ScanItemStatus.valueOf(status)
    @TypeConverter fun fromConfidenceLevel(confidence: ConfidenceLevel): String = confidence.name
    @TypeConverter fun toConfidenceLevel(confidence: String): ConfidenceLevel = ConfidenceLevel.valueOf(confidence)
    @TypeConverter fun fromChangeType(changeType: ChangeType): String = changeType.name
    @TypeConverter fun toChangeType(changeType: String): ChangeType = ChangeType.valueOf(changeType)
    @TypeConverter fun fromRecognitionMode(mode: RecognitionMode): String = mode.name
    @TypeConverter fun toRecognitionMode(mode: String): RecognitionMode = RecognitionMode.valueOf(mode)
    @TypeConverter fun fromRecognitionSource(source: RecognitionSource): String = source.name
    @TypeConverter fun toRecognitionSource(source: String): RecognitionSource = RecognitionSource.valueOf(source)
    @TypeConverter fun fromConfidenceFilter(filter: ConfidenceFilter): String = filter.name
    @TypeConverter fun toConfidenceFilter(filter: String): ConfidenceFilter = ConfidenceFilter.valueOf(filter)
    @TypeConverter fun fromAskBeforeChanging(ask: AskBeforeChanging): String = ask.name
    @TypeConverter fun toAskBeforeChanging(ask: String): AskBeforeChanging = AskBeforeChanging.valueOf(ask)
}
