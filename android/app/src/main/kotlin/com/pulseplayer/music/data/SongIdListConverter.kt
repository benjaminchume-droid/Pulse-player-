package com.pulseplayer.music.data

import androidx.room.TypeConverter

class SongIdListConverter {
    @TypeConverter
    fun fromList(list: List<Long>?): String = list?.joinToString(",") ?: ""

    @TypeConverter
    fun toList(value: String?): List<Long> =
        if (value.isNullOrBlank()) emptyList()
        else value.split(",").mapNotNull { it.trim().toLongOrNull() }
}
