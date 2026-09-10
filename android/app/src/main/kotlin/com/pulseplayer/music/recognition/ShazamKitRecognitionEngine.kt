package com.pulseplayer.music.recognition

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaExtractor
import android.os.Build
import androidx.core.content.ContextCompat
import com.shazam.shazamkit.ShazamKit
import com.shazam.shazamkit.ShazamSession
import com.shazam.shazamkit.catalog.Catalog
import com.shazam.shazamkit.catalog.CatalogQuery
import com.shazam.shazamkit.matchers.AudioMatcher
import com.shazam.shazamkit.matchers.AudioMatcherConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.resume

/**
 * Production-grade ShazamKit recognition engine.
 */
class ShazamKitRecognitionEngine(
    private val context: Context,
    private val apiKey: String
) : RecognitionEngine {

    private var shazamKit: ShazamKit? = null
    private var audioMatcher: AudioMatcher? = null
    private var catalog: Catalog? = null
    private var isInitialized = false

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Initialize ShazamKit with API key
            shazamKit = ShazamKit.Builder(context)
                .setApiKey(apiKey)
                .build()

            // Configure audio matcher
            audioMatcher = shazamKit?.createAudioMatcher(
                AudioMatcherConfiguration.Builder()
                    .setSampleRate(44100)
                    .setChannels(2)
                    .setBitsPerSample(16)
                    .build()
            )

            // Initialize catalog for metadata lookup
            catalog = shazamKit?.createCatalog()

            isInitialized = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun recognizeFromFile(filePath: String): RecognitionResult = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            return@withContext RecognitionResult(
                status = RecognitionStatus.ERROR,
                errorMessage = "Recognition engine not initialized"
            )
        }

        try {
            val audioFile = AudioFileIO.read(File(filePath))
            val audioHeader = audioFile.audioHeader

            // Extract audio samples for recognition
            val samples = extractAudioSamples(filePath)

            if (samples.isEmpty()) {
                return@withContext RecognitionResult(
                    status = RecognitionStatus.ERROR,
                    errorMessage = "Failed to extract audio samples"
                )
            }

            // Perform recognition
            val match = audioMatcher?.match(samples)

            if (match == null || match.matches.isEmpty()) {
                return@withContext RecognitionResult(
                    status = RecognitionStatus.NO_MATCH,
                    confidence = ConfidenceLevel.NO_MATCH
                )
            }

            // Process match
            val bestMatch = match.matches.maxByOrNull { it.confidence } ?: run {
                return@withContext RecognitionResult(
                    status = RecognitionStatus.NO_MATCH,
                    confidence = ConfidenceLevel.NO_MATCH
                )
            }

            // Get metadata from catalog
            val metadata = catalog?.getMetadata(bestMatch.trackId)

            val recognizedMatch = RecognizedMatch(
                id = bestMatch.trackId,
                title = metadata?.title ?: bestMatch.title,
                artist = metadata?.artist ?: bestMatch.artist,
                album = metadata?.album,
                albumArtist = metadata?.albumArtist,
                year = metadata?.releaseYear,
                trackNumber = metadata?.trackNumber,
                discNumber = metadata?.discNumber,
                genre = metadata?.genre,
                composer = metadata?.composer,
                isrc = metadata?.isrc,
                artworkUrl = metadata?.artworkUrl,
                duration = metadata?.duration,
                confidence = evaluateConfidence(bestMatch.confidence),
                isAmbiguous = match.matches.size > 1 && match.matches[0].confidence - match.matches[1].confidence < 0.1,
                provider = "shazam",
                rawMetadata = metadata?.toMap() ?: emptyMap()
            )

            RecognitionResult(
                status = RecognitionStatus.SUCCESS,
                matches = listOf(recognizedMatch),
                confidence = recognizedMatch.confidence
            )
        } catch (e: Exception) {
            e.printStackTrace()
            RecognitionResult(
                status = RecognitionStatus.ERROR,
                errorMessage = e.message
            )
        }
    }

    override suspend fun recognizeFromSamples(samples: FloatArray, sampleRate: Int): RecognitionResult = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            return@withContext RecognitionResult(
                status = RecognitionStatus.ERROR,
                errorMessage = "Recognition engine not initialized"
            )
        }

        try {
            // Convert float samples to short for ShazamKit
            val shortSamples = samples.map { (it * Short.MAX_VALUE).toInt().toShort() }.toShortArray()

            // Perform recognition
            val match = audioMatcher?.match(shortSamples)

            if (match == null || match.matches.isEmpty()) {
                return@withContext RecognitionResult(
                    status = RecognitionStatus.NO_MATCH,
                    confidence = ConfidenceLevel.NO_MATCH
                )
            }

            val bestMatch = match.matches.maxByOrNull { it.confidence } ?: run {
                return@withContext RecognitionResult(
                    status = RecognitionStatus.NO_MATCH,
                    confidence = ConfidenceLevel.NO_MATCH
                )
            }

            val metadata = catalog?.getMetadata(bestMatch.trackId)

            val recognizedMatch = RecognizedMatch(
                id = bestMatch.trackId,
                title = metadata?.title ?: bestMatch.title,
                artist = metadata?.artist ?: bestMatch.artist,
                album = metadata?.album,
                albumArtist = metadata?.albumArtist,
                year = metadata?.releaseYear,
                trackNumber = metadata?.trackNumber,
                discNumber = metadata?.discNumber,
                genre = metadata?.genre,
                composer = metadata?.composer,
                isrc = metadata?.isrc,
                artworkUrl = metadata?.artworkUrl,
                duration = metadata?.duration,
                confidence = evaluateConfidence(bestMatch.confidence),
                isAmbiguous = match.matches.size > 1,
                provider = "shazam",
                rawMetadata = metadata?.toMap() ?: emptyMap()
            )

            RecognitionResult(
                status = RecognitionStatus.SUCCESS,
                matches = listOf(recognizedMatch),
                confidence = recognizedMatch.confidence
            )
        } catch (e: Exception) {
            RecognitionResult(
                status = RecognitionStatus.ERROR,
                errorMessage = e.message
            )
        }
    }

    override suspend fun startMicrophoneRecognition(): RecognitionSession? = withContext(Dispatchers.IO) {
        // Check microphone permission
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext null
        }

        try {
            val session = ShazamKitMicrophoneSession(shazamKit!!, audioMatcher!!)
            session
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun stopRecognition() = withContext(Dispatchers.IO) {
        audioMatcher?.stop()
        shazamKit = null
        audioMatcher = null
        isInitialized = false
    }

    override fun isAvailable(): Boolean = isInitialized

    /**
     * Extract audio samples from file for recognition.
     */
    private suspend fun extractAudioSamples(filePath: String): ShortArray = withContext(Dispatchers.IO) {
        try {
            val mediaExtractor = MediaExtractor()
            val fileDescriptor = FileInputStream(filePath).fd
            mediaExtractor.setDataSource(fileDescriptor)

            // Find audio track
            var audioTrackIndex = -1
            for (i in 0 until mediaExtractor.trackCount) {
                val format = mediaExtractor.getTrackFormat(i)
                val mime = format.getString(MediaExtractor.MIMETYPE_KEY) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    break
                }
            }

            if (audioTrackIndex == -1) {
                return@withContext shortArrayOf()
            }

            mediaExtractor.selectTrack(audioTrackIndex)

            // Extract samples (first 30 seconds for recognition)
            val bufferSize = 1024 * 1024 // 1MB buffer
            val buffer = java.nio.ByteBuffer.allocate(bufferSize)
            val samples = mutableListOf<Short>()

            var maxTimeUs = 30_000_000L // 30 seconds
            var currentTimeUs = 0L

            while (currentTimeUs < maxTimeUs) {
                val sampleSize = mediaExtractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val presentationTimeUs = mediaExtractor.sampleTime
                if (presentationTimeUs > maxTimeUs) break

                buffer.rewind()
                while (buffer.hasRemaining()) {
                    samples.add(buffer.short)
                }

                currentTimeUs = presentationTimeUs
                mediaExtractor.advance()
            }

            mediaExtractor.release()
            samples.toShortArray()
        } catch (e: Exception) {
            e.printStackTrace()
            shortArrayOf()
        }
    }

    /**
     * Evaluate confidence level from raw score.
     */
    private fun evaluateConfidence(rawConfidence: Double): ConfidenceLevel {
        return when {
            rawConfidence >= 0.9 -> ConfidenceLevel.HIGH
            rawConfidence >= 0.7 -> ConfidenceLevel.MEDIUM
            rawConfidence >= 0.5 -> ConfidenceLevel.LOW
            else -> ConfidenceLevel.NO_MATCH
        }
    }
}

/**
 * Extension to convert metadata to map.
 */
private fun com.shazam.shazamkit.catalog.Metadata.toMap(): Map<String, String> {
    return mapOf(
        "title" to (title ?: ""),
        "artist" to (artist ?: ""),
        "album" to (album ?: ""),
        "artworkUrl" to (artworkUrl ?: "")
    )
}
