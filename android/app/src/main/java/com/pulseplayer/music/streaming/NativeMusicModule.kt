package com.pulseplayer.music

import com.pulseplayer.music.data.repository.MusicRepository
import com.pulseplayer.music.data.source.MusicSource
import com.pulseplayer.music.data.source.SourceRegistry

class NativeMusicModule(sources: List<MusicSource>) {
    val sourceRegistry = SourceRegistry(sources)
    val repository = MusicRepository(sourceRegistry)
}
