package com.pulseplayer.music.data.source

import com.pulseplayer.music.domain.ResolveResult
import com.pulseplayer.music.domain.SearchResult
import com.pulseplayer.music.domain.Track

/**
 * Catalogue boundary for YouTube Music. Plug the real authorized/API client
 * into `backend`; no WebView and no HTML scraping are required by this class.
 */
interface YouTubeMusicBackend {
    suspend fun search(query: String, limit: Int): List<Track>
    suspend fun resolveAuthorizedStream(track: Track, purpose: ResolvePurpose): ResolveResult
}

class YouTubeMusicCatalogueSource(private val backend: YouTubeMusicBackend) : MusicSource {
    override val id = "youtube-music"
    override val displayName = "YouTube Music"

    override suspend fun search(query: String, limit: Int): List<SearchResult> =
        backend.search(query, limit).map { SearchResult(it) }

    override suspend fun resolveStream(track: Track, purpose: ResolvePurpose): ResolveResult =
        backend.resolveAuthorizedStream(track, purpose)
}
