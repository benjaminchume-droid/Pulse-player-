package com.pulseplayer.music.data.source

import com.pulseplayer.music.domain.SpotifyMetadata

interface SpotifyMetadataBackend {
    suspend fun search(query: String, limit: Int): List<SpotifyMetadata>
    suspend fun lookup(id: String): SpotifyMetadata?
}

class SpotifyMetadataSource(private val backend: SpotifyMetadataBackend) {
    suspend fun search(query: String, limit: Int = 10) = backend.search(query, limit)
    suspend fun lookup(id: String) = backend.lookup(id)
}
