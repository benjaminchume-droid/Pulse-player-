package com.pulseplayer.music.data.repository

import com.pulseplayer.music.data.source.ResolvePurpose
import com.pulseplayer.music.data.source.SourceRegistry
import com.pulseplayer.music.domain.ResolveResult
import com.pulseplayer.music.domain.Track

class MusicRepository(private val registry: SourceRegistry) {
    suspend fun search(query: String, limitPerSource: Int = 10) =
        registry.all().flatMap { runCatching { it.search(query, limitPerSource) }.getOrDefault(emptyList()) }
            .sortedByDescending { it.score }

    suspend fun resolve(track: Track, purpose: ResolvePurpose): ResolveResult =
        registry.get(track.source)?.resolveStream(track, purpose)
            ?: ResolveResult.Unavailable("Source '${track.source}' is not registered")
}
