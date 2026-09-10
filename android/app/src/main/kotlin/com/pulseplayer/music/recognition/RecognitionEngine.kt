package com.pulseplayer.music.recognition

import com.pulseplayer.music.data.ConfidenceLevel

interface RecognitionEngine {
    suspend fun initialize(): Boolean
    suspend fun recognizeFromFile(filePath: String): RecognitionResult
    suspend fun recognizeFromSamples(samples: ByteArray, sampleRate: Int): RecognitionResult
    suspend fun startMicrophoneRecognition(): RecognitionSession?
    suspend fun stopRecognition()
    fun isAvailable(): Boolean
    fun providerName(): String
}

data class RecognitionResult(
    val status: RecognitionStatus,
    val matches: List<RecognizedMatch> = emptyList(),
    val confidence: ConfidenceLevel = ConfidenceLevel.NO_MATCH,
    val errorMessage: String? = null
)

enum class RecognitionStatus {
    SUCCESS, NO_MATCH, AMBIGUOUS, ERROR, TIMEOUT, NETWORK_ERROR, PERMISSION_DENIED, UNAVAILABLE
}

data class RecognizedMatch(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val albumArtist: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val genre: String? = null,
    val composer: String? = null,
    val isrc: String? = null,
    val artworkUrl: String? = null,
    val durationMs: Long? = null,
    val confidence: ConfidenceLevel,
    val isAmbiguous: Boolean = false,
    val provider: String,
    val rawMetadata: Map<String, String> = emptyMap()
) {
    fun isUsableTitle(): Boolean {
        val t = title.trim()
        if (t.isEmpty()) return false
        if (t.all { it.isDigit() || it == '-' }) return false
        if (t.matches(Regex("^\\d{5,}$"))) return false
        return true
    }
}

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
