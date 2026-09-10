package com.pulseplayer.music.metadata

import com.pulseplayer.music.recognition.RecognizedMatch

data class CanonicalTrackMetadata(
    val title: String,
    val artist: String,
    val album: String? = null,
    val albumArtist: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val genre: String? = null,
    val artworkUrl: String? = null,
    val durationMs: Long? = null,
    val provider: String,
    val confidenceLabel: String
)

object MetadataResolver {
    fun fromMatch(match: RecognizedMatch): CanonicalTrackMetadata? {
        if (!match.isUsableTitle()) return null
        return CanonicalTrackMetadata(
            title = match.title.trim(),
            artist = match.artist.trim().ifBlank { "Unknown Artist" },
            album = match.album,
            albumArtist = match.albumArtist,
            year = match.year,
            trackNumber = match.trackNumber,
            genre = match.genre,
            artworkUrl = match.artworkUrl,
            durationMs = match.durationMs,
            provider = match.provider,
            confidenceLabel = match.confidence.name
        )
    }
}
