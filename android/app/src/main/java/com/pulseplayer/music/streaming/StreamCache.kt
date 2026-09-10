package com.pulseplayer.music.cache

import com.pulseplayer.music.domain.StreamResolution
import java.util.concurrent.ConcurrentHashMap

class StreamCache {
    private val values = ConcurrentHashMap<String, StreamResolution>()
    fun get(key: String): StreamResolution? = values[key]?.takeUnless { it.expired() } ?: run { values.remove(key); null }
    fun put(key: String, value: StreamResolution) { values[key] = value }
    fun clear() = values.clear()
}
