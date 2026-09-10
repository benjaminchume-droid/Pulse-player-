package com.pulseplayer.music.domain

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val artworkUrl: String? = null,
    val source: String,
    val sourceId: String,
    val isrc: String? = null,
    val externalUrl: String? = null,
    val explicit: Boolean = false
)

data class StreamResolution(
    val url: String,
    val source: String,
    val mimeType: String? = null,
    val bitrate: Int? = null,
    val contentLength: Long? = null,
    val expiresAtEpochMs: Long? = null,
    val headers: Map<String, String> = emptyMap()
) {
    fun expired(now: Long = System.currentTimeMillis()) = expiresAtEpochMs?.let { now >= it } == true
}

data class SearchResult(val track: Track, val score: Int = 0)

data class SpotifyMetadata(
    val id: String?, val title: String, val artist: String, val album: String?,
    val durationMs: Long?, val artworkUrl: String?, val isrc: String?, val externalUrl: String?
)

sealed interface ResolveResult {
    data class Success(val value: StreamResolution) : ResolveResult
    data class Unavailable(val reason: String) : ResolveResult
}
