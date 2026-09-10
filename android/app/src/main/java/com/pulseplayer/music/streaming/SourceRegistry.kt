package com.pulseplayer.music.data.source

class SourceRegistry(sources: List<MusicSource>) {
    private val byId = sources.associateBy { it.id }
    fun get(id: String): MusicSource? = byId[id]
    fun all(): List<MusicSource> = byId.values.toList()
}
