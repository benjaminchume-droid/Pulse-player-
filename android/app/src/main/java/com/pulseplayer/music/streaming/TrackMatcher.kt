package com.pulseplayer.music.data.repository

import com.pulseplayer.music.domain.SpotifyMetadata
import com.pulseplayer.music.domain.Track
import kotlin.math.abs

class TrackMatcher {
    fun score(a: Track, b: SpotifyMetadata): Int {
        var s = 0
        if (!a.isrc.isNullOrBlank() && a.isrc.equals(b.isrc, true)) s += 40
        if (norm(a.title) == norm(b.title)) s += 25
        if (norm(a.artist) == norm(b.artist)) s += 20
        if (a.durationMs > 0 && (b.durationMs ?: 0) > 0 && abs(a.durationMs - (b.durationMs ?: 0)) <= 2000) s += 10
        if (a.album.isNotBlank() && b.album != null && norm(a.album) == norm(b.album)) s += 5
        return s
    }
    private fun norm(v: String) = v.lowercase().replace(Regex("\\([^)]*\\)|\\[[^]]*\\]"), " ")
        .replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()
}
