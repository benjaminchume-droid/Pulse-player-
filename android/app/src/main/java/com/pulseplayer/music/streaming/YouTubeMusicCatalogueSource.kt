package com.pulseplayer.music.data.source

import com.pulseplayer.music.domain.ResolveResult
import com.pulseplayer.music.domain.SearchResult
import com.pulseplayer.music.domain.Track

/**
 * Catalogue boundary for YouTube Music.
 * Default backend returns open-external search only (no DRM stream scrape).
 */
interface YouTubeMusicBackend {
    suspend fun search(query: String, limit: Int): List<Track>
    suspend fun resolveAuthorizedStream(track: Track, purpose: ResolvePurpose): ResolveResult
}

class OpenExternalYouTubeBackend : YouTubeMusicBackend {
    override suspend fun search(query: String, limit: Int): List<Track> {
        val q = java.net.URLEncoder.encode(query, "UTF-8")
        return listOf(
            Track(
                id = "ytm_$q",
                title = "Search YouTube Music: $query",
                artist = "Opens externally",
                source = "youtube-music",
                sourceId = q,
                externalUrl = "https://music.youtube.com/search?q=$q"
            )
        )
    }

    override suspend fun resolveAuthorizedStream(track: Track, purpose: ResolvePurpose): ResolveResult =
        ResolveResult.Unavailable("In-app YTM stream requires official licensed API; open externally")
}

class YouTubeMusicCatalogueSource(
    private val backend: YouTubeMusicBackend = OpenExternalYouTubeBackend()
) : MusicSource {
    override val id = "youtube-music"
    override val displayName = "YouTube Music"

    override suspend fun search(query: String, limit: Int): List<SearchResult> =
        backend.search(query, limit).map { SearchResult(it) }

    override suspend fun resolveStream(track: Track, purpose: ResolvePurpose): ResolveResult =
        backend.resolveAuthorizedStream(track, purpose)
}
