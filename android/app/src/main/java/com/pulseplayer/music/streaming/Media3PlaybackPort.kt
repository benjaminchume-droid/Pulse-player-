package com.pulseplayer.music.playback

import android.net.Uri
import com.pulseplayer.music.domain.StreamResolution
import com.pulseplayer.music.domain.Track

interface Media3PlaybackPort {
    fun play(track: Track, resolution: StreamResolution)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setQueue(items: List<Pair<Track, StreamResolution>>, startIndex: Int = 0)
    fun currentPositionMs(): Long
    fun isPlaying(): Boolean
}

fun StreamResolution.uri(): Uri = Uri.parse(url)
