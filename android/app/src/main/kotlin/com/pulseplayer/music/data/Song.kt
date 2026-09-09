package com.pulseplayer.music.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val duration: Long = 0L,
    val filePath: String? = null,
    val albumArt: String? = null,
    val sourceUrl: String? = null,
    val source: String = "local",
    val isDownloaded: Boolean = false,
    val downloadedAt: Long? = null,
    val localFilePath: String? = null
)
