package com.pulseplayer.music.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class SongIdListConverter {
    @TypeConverter
    fun fromList(list: List<Long>): String = list.joinToString(",")

    @TypeConverter
    fun toList(value: String): List<Long> =
        if (value.isBlank()) emptyList()
        else value.split(",").mapNotNull { it.trim().toLongOrNull() }
}

@Entity(tableName = "playlists")
@TypeConverters(SongIdListConverter::class)
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val songIds: List<Long> = emptyList(),
    val coverUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
