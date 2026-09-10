package com.pulseplayer.music.metadata

import com.pulseplayer.music.data.repository.TrackMatcher
import com.pulseplayer.music.data.source.SpotifyMetadataSource
import com.pulseplayer.music.domain.Track

class SpotifyEnricher(
    private val source: SpotifyMetadataSource,
    private val matcher: TrackMatcher = TrackMatcher()
) {
    suspend fun enrich(track: Track): Track {
        val best = source.search("${track.title} ${track.artist}", 8)
            .map { it to matcher.score(track, it) }
            .maxByOrNull { it.second }
            ?.takeIf { it.second >= 70 }?.first ?: return track
        return track.copy(
            title = best.title,
            artist = best.artist,
            album = best.album ?: track.album,
            durationMs = best.durationMs ?: track.durationMs,
            artworkUrl = best.artworkUrl ?: track.artworkUrl,
            isrc = best.isrc ?: track.isrc,
            externalUrl = best.externalUrl ?: track.externalUrl
        )
    }
}
