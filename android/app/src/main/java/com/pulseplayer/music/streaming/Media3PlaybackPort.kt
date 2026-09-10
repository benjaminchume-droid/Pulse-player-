package com.pulseplayer.music.playback

import com.pulseplayer.music.domain.StreamResolution
import com.pulseplayer.music.domain.Track

/**
 * Boundary for a future Media3/ExoPlayer implementation.
 * Current app playback uses [com.pulseplayer.music.service.PlaybackService].
 */
interface Media3PlaybackPort {
    fun play(track: Track, stream: StreamResolution)
    fun pause()
    fun stop()
}

class NoOpMedia3PlaybackPort : Media3PlaybackPort {
    override fun play(track: Track, stream: StreamResolution) {}
    override fun pause() {}
    override fun stop() {}
}
