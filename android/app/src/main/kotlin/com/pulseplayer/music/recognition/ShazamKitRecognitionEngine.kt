package com.pulseplayer.music.recognition

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ShazamKit integration point (official Apple Android SDK).
 *
 * The proprietary AAR is NOT on Maven Central. To enable:
 * 1. Download shazamkit-android-release.aar from developer.apple.com/shazamkit/android/
 * 2. Place at android/app/libs/shazamkit-android-release.aar
 * 3. Create Media ID + private key; generate developer token (max 6 months)
 * 4. Set SHAZAM_DEVELOPER_TOKEN env / secret
 *
 * Until the AAR is present this engine reports UNAVAILABLE and the app uses AudDRecognitionEngine.
 *
 * Official entry points (when linked):
 * - ShazamKit.createShazamCatalog(developerTokenProvider)
 * - ShazamKit.createSignatureGenerator(sampleRate)
 * - ShazamKit.createStreamingSession(...)
 *
 * Do not scrape Shazam. Do not invent API keys.
 */
class ShazamKitRecognitionEngine(
    private val context: Context,
    private val developerToken: String
) : RecognitionEngine {

    private val aarPresent: Boolean =
        File(context.applicationInfo.nativeLibraryDir).parentFile
            ?.resolve("libs")?.exists() == true ||
            File(context.filesDir.parentFile, "libs/shazamkit-android-release.aar").exists() ||
            File("libs/shazamkit-android-release.aar").exists()

    @Volatile private var available = false

    override fun providerName() = "shazamkit"

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        available = developerToken.isNotBlank() && isAarOnClasspath()
        available
    }

    private fun isAarOnClasspath(): Boolean {
        return try {
            Class.forName("com.shazam.shazamkit.ShazamKit")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    override fun isAvailable(): Boolean = available

    override suspend fun recognizeFromFile(filePath: String): RecognitionResult {
        if (!available) {
            return RecognitionResult(
                RecognitionStatus.UNAVAILABLE,
                errorMessage = "ShazamKit AAR not linked or developer token missing. Using fallback provider."
            )
        }
        // When AAR is linked, integrate createSignatureGenerator + catalog match here.
        // Kept explicit UNAVAILABLE path so the project compiles without the proprietary AAR.
        return RecognitionResult(
            RecognitionStatus.UNAVAILABLE,
            errorMessage = "ShazamKit runtime bridge not configured in this build"
        )
    }

    override suspend fun recognizeFromSamples(samples: ByteArray, sampleRate: Int): RecognitionResult {
        return RecognitionResult(
            RecognitionStatus.UNAVAILABLE,
            errorMessage = "ShazamKit not active"
        )
    }

    override suspend fun startMicrophoneRecognition(): RecognitionSession? = null

    override suspend fun stopRecognition() {}
}
