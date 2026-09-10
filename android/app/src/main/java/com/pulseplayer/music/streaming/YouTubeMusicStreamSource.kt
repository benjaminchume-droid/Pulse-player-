package com.pulseplayer.music.data.source

import com.pulseplayer.music.domain.ResolveResult
import com.pulseplayer.music.domain.Track

/** Explicit resolver boundary. It must be backed by an authorized media resolver. */
class YouTubeMusicStreamSource(private val resolver: YouTubeMusicBackend) {
    suspend fun resolve(track: Track, purpose: ResolvePurpose): ResolveResult =
        resolver.resolveAuthorizedStream(track, purpose)
}
