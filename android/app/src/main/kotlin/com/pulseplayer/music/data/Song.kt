package com.pulseplayer.music.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val duration: Long = 0L,
    val path: String,
    val genre: String = "All Streams",
    var isFavorite: Boolean = false,
    var playCount: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
    val albumArtist: String = "",
    val year: Int = 0,
    val trackNumber: Int = 0,
    val bitrate: Int = 0,
    val coverUrl: String = "",
    val lyrics: String = "",
    val syncedLyrics: String = "",
    val metadataEnriched: Boolean = false,
    /** local | stream | download */
    val sourceType: String = "local",
    val sourceId: String = "",
    val isDownloaded: Boolean = false
)

enum class RepeatMode { OFF, ONE, ALL }
