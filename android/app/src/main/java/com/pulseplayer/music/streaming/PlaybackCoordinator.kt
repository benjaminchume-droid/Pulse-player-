package com.pulseplayer.music.playback

import com.pulseplayer.music.data.source.ResolvePurpose
import com.pulseplayer.music.data.repository.MusicRepository
import com.pulseplayer.music.domain.ResolveResult
import com.pulseplayer.music.domain.Track

class PlaybackCoordinator(
    private val repository: MusicRepository,
    private val player: Media3PlaybackPort
) {
    suspend fun play(track: Track): ResolveResult {
        val resolved = repository.resolve(track, ResolvePurpose.PLAYBACK)
        if (resolved is ResolveResult.Success) player.play(track, resolved.value)
        return resolved
    }
}
