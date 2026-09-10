package com.pulseplayer.music.metadata

import com.pulseplayer.music.data.ConfidenceLevel
import com.pulseplayer.music.recognition.RecognizedMatch

/**
 * Resolves recognition results into canonical metadata.
 */
object MetadataResolver {

    /**
     * Resolve a recognized match into canonical track metadata.
     */
    fun resolve(match: RecognizedMatch): CanonicalTrackMetadata {
        return CanonicalTrackMetadata(
            title = match.title.trim(),
            artist = match.artist.trim(),
            album = match.album?.trim(),
            albumArtist = match.albumArtist?.trim() ?: match.artist,
            year = match.year,
            trackNumber = match.trackNumber,
            discNumber = match.discNumber,
            genre = match.genre,
            composer = match.composer,
            isrc = match.isrc,
            artworkUrl = match.artworkUrl,
            duration = match.duration,
            source = match.provider,
            recordingId = match.id,
            confidence = match.confidence,
            isAmbiguous = match.isAmbiguous
        )
    }

    /**
     * Normalize metadata fields.
     */
    fun normalize(metadata: CanonicalTrackMetadata): CanonicalTrackMetadata {
        return metadata.copy(
            title = metadata.title.normalizeTitle(),
            artist = metadata.artist.normalizeArtist(),
            album = metadata.album?.normalizeAlbum(),
            genre = metadata.genre?.normalizeGenre()
        )
    }

    private fun String.normalizeTitle(): String {
        return this
            .replace(Regex("\\s+"), " ")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun String.normalizeArtist(): String {
        return this
            .replace(Regex("\\s+"), " ")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun String?.normalizeAlbum(): String? {
        return this?.replace(Regex("\\s+"), " ")?.trim()
    }

    private fun String?.normalizeGenre(): String? {
        return this?.trim()
    }
}

/**
 * Canonical track metadata after resolution.
 */
data class CanonicalTrackMetadata(
    val title: String,
    val artist: String,
    val album: String?,
    val albumArtist: String?,
    val year: Int?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val genre: String?,
    val composer: String?,
    val isrc: String?,
    val artworkUrl: String?,
    val duration: Long?,
    val source: String,
    val recordingId: String,
    val confidence: ConfidenceLevel,
    val isAmbiguous: Boolean
)
