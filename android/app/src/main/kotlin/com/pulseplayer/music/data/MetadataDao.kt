package com.pulseplayer.music.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MetadataDao {

    // ===== Scan Jobs =====
    @Query("SELECT * FROM metadata_scan_jobs ORDER BY createdAt DESC")
    fun getAllScanJobs(): Flow<List<MetadataScanJob>>

    @Query("SELECT * FROM metadata_scan_jobs WHERE id = :jobId")
    suspend fun getScanJob(jobId: String): MetadataScanJob?

    @Query("SELECT * FROM metadata_scan_jobs WHERE status IN ('RUNNING', 'PAUSED') LIMIT 1")
    suspend fun getActiveScanJob(): MetadataScanJob?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanJob(job: MetadataScanJob)

    @Update
    suspend fun updateScanJob(job: MetadataScanJob)

    @Delete
    suspend fun deleteScanJob(job: MetadataScanJob)

    // ===== Scan Items =====
    @Query("SELECT * FROM metadata_scan_items WHERE jobId = :jobId ORDER BY createdAt ASC")
    fun getScanItemsForJob(jobId: String): Flow<List<MetadataScanItem>>

    @Query("SELECT * FROM metadata_scan_items WHERE jobId = :jobId AND status = 'QUEUED' LIMIT :limit")
    suspend fun getQueuedScanItems(jobId: String, limit: Int = 10): List<MetadataScanItem>

    @Query("SELECT * FROM metadata_scan_items WHERE id = :itemId")
    suspend fun getScanItem(itemId: String): MetadataScanItem?

    @Query("SELECT COUNT(*) FROM metadata_scan_items WHERE jobId = :jobId")
    suspend fun getScanItemCount(jobId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanItem(item: MetadataScanItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanItems(items: List<MetadataScanItem>)

    @Update
    suspend fun updateScanItem(item: MetadataScanItem)

    @Delete
    suspend fun deleteScanItem(item: MetadataScanItem)

    // ===== Matches =====
    @Query("SELECT * FROM metadata_matches WHERE scanItemId = :scanItemId ORDER BY createdAt DESC")
    fun getMatchesForScanItem(scanItemId: String): Flow<List<MetadataMatch>>

    @Query("SELECT * FROM metadata_matches WHERE id = :matchId")
    suspend fun getMatch(matchId: String): MetadataMatch?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MetadataMatch)

    @Delete
    suspend fun deleteMatch(match: MetadataMatch)

    // ===== Changes =====
    @Query("SELECT * FROM metadata_changes WHERE songId = :songId ORDER BY createdAt DESC")
    fun getChangesForSong(songId: String): Flow<List<MetadataChange>>

    @Query("SELECT * FROM metadata_changes WHERE applied = 1 AND canRollback = 1 ORDER BY createdAt DESC")
    fun getAppliedChangesWithRollback(): Flow<List<MetadataChange>>

    @Query("SELECT * FROM metadata_changes WHERE id = :changeId")
    suspend fun getChange(changeId: String): MetadataChange?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChange(change: MetadataChange)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChanges(changes: List<MetadataChange>)

    @Update
    suspend fun updateChange(change: MetadataChange)

    // ===== Settings =====
    @Query("SELECT * FROM metadata_settings WHERE id = 'default'")
    fun getSettings(): Flow<MetadataSettings>

    @Query("SELECT * FROM metadata_settings WHERE id = 'default'")
    suspend fun getSettingsSync(): MetadataSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: MetadataSettings)

    @Update
    suspend fun updateSettings(settings: MetadataSettings)

    // ===== Artwork Cache =====
    @Query("SELECT * FROM artwork_cache WHERE recordingId = :recordingId")
    suspend fun getArtworkForRecording(recordingId: String): ArtworkCache?

    @Query("SELECT * FROM artwork_cache ORDER BY lastAccessedAt DESC")
    fun getAllArtwork(): Flow<List<ArtworkCache>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtwork(artwork: ArtworkCache)

    @Delete
    suspend fun deleteArtwork(artwork: ArtworkCache)

    @Query("UPDATE artwork_cache SET lastAccessedAt = :timestamp WHERE id = :id")
    suspend fun updateArtworkAccess(id: String, timestamp: Long)

    // ===== Cleanup =====
    @Query("DELETE FROM metadata_scan_items WHERE jobId = :jobId")
    suspend fun deleteScanItemsForJob(jobId: String)

    @Query("DELETE FROM metadata_matches WHERE scanItemId IN (SELECT id FROM metadata_scan_items WHERE jobId = :jobId)")
    suspend fun deleteMatchesForJob(jobId: String)

    @Query("DELETE FROM metadata_changes WHERE songId IN (SELECT DISTINCT songId FROM metadata_changes WHERE createdAt < :beforeDate)")
    suspend fun cleanupOldChanges(beforeDate: Long)

    @Query("DELETE FROM artwork_cache WHERE expiresAt IS NOT NULL AND expiresAt < :currentTime")
    suspend fun cleanupExpiredArtwork(currentTime: Long)
}
