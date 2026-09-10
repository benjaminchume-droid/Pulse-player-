package com.pulseplayer.music.recognition

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import java.io.File
import java.io.FileInputStream

/**
 * Main recognition engine interface.
 */
interface RecognitionEngine {
    /**
     * Initialize the recognition engine.
     */
    suspend fun initialize(): Boolean

    /**
     * Recognize audio from a file.
     */
    suspend fun recognizeFromFile(filePath: String): RecognitionResult

    /**
     * Recognize audio from PCM samples (for live playback).
     */
    suspend fun recognizeFromSamples(samples: FloatArray, sampleRate: Int): RecognitionResult

    /**
     * Start microphone recognition session.
     */
    suspend fun startMicrophoneRecognition(): RecognitionSession?

    /**
     * Stop recognition and release resources.
     */
    suspend fun stopRecognition()

    /**
     * Check if the engine is available.
     */
    fun isAvailable(): Boolean
}

/**
 * Result from a recognition operation.
 */
data class RecognitionResult(
    val status: RecognitionStatus,
    val matches: List<RecognizedMatch> = emptyList(),
    val confidence: ConfidenceLevel = ConfidenceLevel.NO_MATCH,
    val errorMessage: String? = null
)

enum class RecognitionStatus {
    SUCCESS,
    NO_MATCH,
    AMBIGUOUS,
    ERROR,
    TIMEOUT,
    NETWORK_ERROR,
    PERMISSION_DENIED
}

/**
 * Individual recognized match.
 */
data class RecognizedMatch(
    val id: String,
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
    val confidence: ConfidenceLevel,
    val isAmbiguous: Boolean,
    val provider: String,
    val rawMetadata: Map<String, String>
)

/**
 * Active recognition session for microphone input.
 */
interface RecognitionSession {
    suspend fun start()
    suspend fun stop()
    suspend fun isRecognizing(): Boolean
    fun setListener(listener: RecognitionListener)
}

interface RecognitionListener {
    fun onRecognitionStarted()
    fun onMatchFound(match: RecognizedMatch)
    fun onRecognitionError(error: String)
    fun onRecognitionStopped()
}
