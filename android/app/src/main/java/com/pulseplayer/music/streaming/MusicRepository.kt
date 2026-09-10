package com.pulseplayer.music.data.repository

import com.pulseplayer.music.data.source.MusicSource
import com.pulseplayer.music.data.source.ResolvePurpose
import com.pulseplayer.music.data.source.SourceRegistry
import com.pulseplayer.music.domain.ResolveResult
import com.pulseplayer.music.domain.SearchResult
import com.pulseplayer.music.domain.Track

/**
 * Thin facade over registered [MusicSource] plugins.
 * Does not scrape DRM services; sources return authorized or open-external results only.
 */
class MusicRepository(private val registry: SourceRegistry) {
    fun sources(): List<MusicSource> = registry.all()

    suspend fun search(query: String, limit: Int = 20): List<SearchResult> =
        registry.all().flatMap { src ->
            try { src.search(query, limit) } catch (_: Exception) { emptyList() }
        }

    suspend fun resolve(track: Track, purpose: ResolvePurpose): ResolveResult {
        val src = registry.get(track.source) ?: return ResolveResult.Unavailable("Unknown source")
        return try {
            src.resolveStream(track, purpose)
        } catch (e: Exception) {
            ResolveResult.Unavailable(e.message ?: "resolve failed")
        }
    }
}
