package com.pulseplayer.music.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val songIds: List<Long> = emptyList(), // Store as list of song IDs
    val createdAt: Long = System.currentTimeMillis()
)

class Converters {
    @TypeConverter
    fun fromString(value: String?): List<Long> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(",").mapNotNull { it.toLongOrNull() }
    }

    @TypeConverter
    fun fromList(list: List<Long>?): String {
        return list?.joinToString(",") ?: ""
    }
}
