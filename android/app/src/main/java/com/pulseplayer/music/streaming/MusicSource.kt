package com.pulseplayer.music.data.source

import com.pulseplayer.music.domain.ResolveResult
import com.pulseplayer.music.domain.SearchResult
import com.pulseplayer.music.domain.StreamResolution
import com.pulseplayer.music.domain.Track

enum class ResolvePurpose { PLAYBACK, DOWNLOAD }

interface MusicSource {
    val id: String
    val displayName: String
    suspend fun search(query: String, limit: Int = 20): List<SearchResult>
    suspend fun resolveStream(track: Track, purpose: ResolvePurpose): ResolveResult
}
