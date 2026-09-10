package com.pulseplayer.music.metadata

import android.content.Context
import android.media.MediaScannerConnection
import androidx.documentfile.provider.DocumentFile
import com.pulseplayer.music.data.MetadataChange
import com.pulseplayer.music.data.ChangeType
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.mp3.MP3File
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.id3.ID3v24Tag
import org.jaudiotagger.tag.mp4.MP4Tag
import org.jaudiotagger.tag.flac.FlacTag
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL

/**
 * Writes metadata to audio files safely.
 */
class MetadataWriter(private val context: Context) {

    /**
     * Write metadata to audio file.
     */
    suspend fun writeMetadata(
        filePath: String,
        metadata: CanonicalTrackMetadata,
        changeSet: MetadataChangeSet
    ): WriteMetadataResult {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return WriteMetadataResult.Failure("File not found: $filePath")
            }

            // Create temp file for safe modification
            val tempFile = File.createTempFile("metadata_", "_${file.extension}", file.parentFile)
            
            try {
                // Copy original to temp
                file.copyTo(tempFile, overwrite = true)

                // Write metadata to temp file
                val audioFile = AudioFileIO.read(tempFile)
                writeTags(audioFile, metadata, changeSet)
                
                // Write artwork if needed
                if (changeSet.shouldAddArtwork && metadata.artworkUrl != null) {
                    writeArtwork(audioFile, metadata.artworkUrl)
                }

                // Commit changes
                audioFile.commit()

                // Validate the file
                if (!validateFile(tempFile)) {
                    tempFile.delete()
                    return WriteMetadataResult.Failure("Invalid file after metadata write")
                }

                // Atomically replace original
                file.delete()
                tempFile.renameTo(file)

                // Re-index MediaStore
                reindexMediaStore(file)

                WriteMetadataResult.Success(file)
            } catch (e: Exception) {
                tempFile.delete()
                WriteMetadataResult.Failure("Failed to write metadata: ${e.message}")
            }
        } catch (e: Exception) {
            WriteMetadataResult.Failure("Error: ${e.message}")
        }
    }

    private fun writeTags(audioFile: AudioFile, metadata: CanonicalTrackMetadata, changeSet: MetadataChangeSet) {
        val tag = audioFile.tag

        changeSet.changes.forEach { change ->
            when (change.field) {
                MetadataField.TITLE -> tag.setField(FieldKey.TITLE, change.newValue ?: "")
                MetadataField.ARTIST -> tag.setField(FieldKey.ARTIST, change.newValue ?: "")
                MetadataField.ALBUM -> tag.setField(FieldKey.ALBUM, change.newValue ?: "")
                MetadataField.YEAR -> change.newValue?.toIntOrNull()?.let { year ->
                    tag.setField(FieldKey.YEAR, year.toString())
                }
            }
        }

        // Additional fields
        metadata.albumArtist?.let { tag.setField(FieldKey.ALBUM_ARTIST, it) }
        metadata.genre?.let { tag.setField(FieldKey.GENRE, it) }
        metadata.composer?.let { tag.setField(FieldKey.COMPOSER, it) }
        metadata.trackNumber?.let { tag.setField(FieldKey.TRACK, it.toString()) }
        metadata.discNumber?.let { tag.setField(FieldKey.DISC_NO, it.toString()) }
        metadata.isrc?.let { tag.setField(FieldKey.ISRC, it) }
    }

    private suspend fun writeArtwork(audioFile: AudioFile, artworkUrl: String) {
        try {
            val url = URL(artworkUrl)
            val artworkBytes = url.readBytes()
            
            // Resize if needed (max 1000x1000)
            val resizedArtwork = resizeArtwork(artworkBytes, 1000, 1000)

            audioFile.tag.setField(FieldKey.COVER_ART, resizedArtwork)
        } catch (e: Exception) {
            // Artwork write failed, but continue with metadata
            e.printStackTrace()
        }
    }

    private fun resizeArtwork(artworkBytes: ByteArray, maxWidth: Int, maxHeight: Int): ByteArray {
        // TODO: Implement actual image resizing
        // For now, return original
        return artworkBytes
    }

    private fun validateFile(file: File): Boolean {
        return try {
            val audioFile = AudioFileIO.read(file)
            audioFile.audioHeader.duration > 0
        } catch (e: Exception) {
            false
        }
    }

    private fun reindexMediaStore(file: File) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            null
        ) { path, uri ->
            // Scan complete
        }
    }
}

sealed class WriteMetadataResult {
    data class Success(val file: File) : WriteMetadataResult()
    data class Failure(val error: String) : WriteMetadataResult()
}
