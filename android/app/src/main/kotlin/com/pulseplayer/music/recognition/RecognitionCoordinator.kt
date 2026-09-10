package com.pulseplayer.music.recognition

import android.content.Context
import com.pulseplayer.music.BuildConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Chooses ShazamKit when AAR + token exist; otherwise AudD.
 * Tracks per-file cooldown so we do not re-recognize confidently identified tracks.
 */
class RecognitionCoordinator(context: Context) {

    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private val completed = mutableMapOf<String, Long>() // key -> timestamp
    private val cooldownMs = 7L * 24 * 60 * 60 * 1000 // 7 days

    private val shazam = ShazamKitRecognitionEngine(
        appContext,
        BuildConfig.SHAZAM_DEVELOPER_TOKEN
    )
    private val audd = AudDRecognitionEngine(
        appContext,
        BuildConfig.AUDD_API_TOKEN
    )

    private var primary: RecognitionEngine = audd

    suspend fun initialize() {
        val shazamOk = shazam.initialize()
        val auddOk = audd.initialize()
        primary = when {
            shazamOk && shazam.isAvailable() -> shazam
            auddOk && audd.isAvailable() -> audd
            else -> audd
        }
    }

    fun activeProvider(): String = primary.providerName()

    fun isReady(): Boolean = primary.isAvailable()

    suspend fun recognizeFile(stableKey: String, filePath: String, force: Boolean = false): RecognitionResult =
        mutex.withLock {
            val last = completed[stableKey]
            if (!force && last != null && System.currentTimeMillis() - last < cooldownMs) {
                return@withLock RecognitionResult(
                    RecognitionStatus.SUCCESS,
                    confidence = ConfidenceLevel.HIGH,
                    errorMessage = "skipped_cooldown"
                )
            }
            var result = primary.recognizeFromFile(filePath)
            if (result.status == RecognitionStatus.UNAVAILABLE && primary !== audd) {
                result = audd.recognizeFromFile(filePath)
            }
            // Never accept numeric-only titles
            val cleaned = result.matches.filter { it.isUsableTitle() }
            if (result.status == RecognitionStatus.SUCCESS && cleaned.isEmpty()) {
                return@withLock RecognitionResult(RecognitionStatus.NO_MATCH, confidence = ConfidenceLevel.NO_MATCH)
            }
            val finalResult = result.copy(matches = cleaned.ifEmpty { result.matches })
            if (finalResult.status == RecognitionStatus.SUCCESS &&
                finalResult.confidence == ConfidenceLevel.HIGH
            ) {
                completed[stableKey] = System.currentTimeMillis()
            }
            finalResult
        }

    suspend fun recognizeWhilePlaying(stableKey: String, path: String): RecognitionResult =
        recognizeFile(stableKey, path, force = false)
}
