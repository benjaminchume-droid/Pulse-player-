package com.pulseplayer.music.recognition

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.pulseplayer.music.data.ConfidenceLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class AudDRecognitionEngine(
    private val context: Context,
    private val apiToken: String
) : RecognitionEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    @Volatile private var ready = false
    private var micSession: MicSession? = null

    override fun providerName() = "audd"

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        ready = apiToken.isNotBlank()
        ready
    }

    override fun isAvailable(): Boolean = ready

    override suspend fun recognizeFromFile(filePath: String): RecognitionResult = withContext(Dispatchers.IO) {
        if (!ready) {
            return@withContext RecognitionResult(
                RecognitionStatus.UNAVAILABLE,
                errorMessage = "AudD token not configured (AUDD_API_TOKEN)"
            )
        }
        val file = File(filePath)
        if (!file.exists() || !file.canRead()) {
            return@withContext recognizeContentOrPath(filePath)
        }
        postSample(file)
    }

    private suspend fun recognizeContentOrPath(path: String): RecognitionResult {
        return try {
            if (path.startsWith("content://")) {
                val tmp = File(context.cacheDir, "recognize_${System.currentTimeMillis()}.bin")
                context.contentResolver.openInputStream(android.net.Uri.parse(path))?.use { input ->
                    tmp.outputStream().use { output -> input.copyTo(output) }
                } ?: return RecognitionResult(RecognitionStatus.ERROR, errorMessage = "Cannot read content URI")
                val result = postSample(tmp)
                tmp.delete()
                result
            } else {
                RecognitionResult(RecognitionStatus.ERROR, errorMessage = "File not readable")
            }
        } catch (e: Exception) {
            RecognitionResult(RecognitionStatus.ERROR, errorMessage = e.message)
        }
    }

    private fun postSample(file: File): RecognitionResult {
        if (file.length() > 12_000_000) {
            return RecognitionResult(RecognitionStatus.ERROR, errorMessage = "Sample too large")
        }
        return try {
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("api_token", apiToken)
                .addFormDataPart("return", "apple_music,spotify")
                .addFormDataPart("file", file.name, file.asRequestBody("application/octet-stream".toMediaType()))
                .build()
            val req = Request.Builder().url("https://api.audd.io/").post(body).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return RecognitionResult(RecognitionStatus.NETWORK_ERROR, errorMessage = "HTTP ${resp.code}")
                }
                val json = JSONObject(resp.body?.string().orEmpty())
                if (json.optString("status") != "success") {
                    return RecognitionResult(RecognitionStatus.ERROR, errorMessage = json.optString("error", "AudD error"))
                }
                val result = json.optJSONObject("result")
                    ?: return RecognitionResult(RecognitionStatus.NO_MATCH, confidence = ConfidenceLevel.NO_MATCH)

                val title = result.optString("title").trim()
                val artist = result.optString("artist").trim()
                if (title.isEmpty() || title.all { it.isDigit() }) {
                    return RecognitionResult(RecognitionStatus.NO_MATCH, confidence = ConfidenceLevel.NO_MATCH)
                }

                val apple = result.optJSONObject("apple_music")
                val spotify = result.optJSONObject("spotify")
                val artwork = apple?.optString("artworkUrl")?.replace("{w}", "600")?.replace("{h}", "600")
                    ?: spotify?.optJSONObject("album")?.optJSONArray("images")?.optJSONObject(0)?.optString("url")

                val year = result.optString("release_date").take(4).toIntOrNull()
                    ?: apple?.optString("releaseDate")?.take(4)?.toIntOrNull()

                val match = RecognizedMatch(
                    id = result.optString("song_link").ifBlank { "$artist-$title".hashCode().toString() },
                    title = title,
                    artist = artist.ifBlank { "Unknown Artist" },
                    album = result.optString("album").ifBlank { null },
                    year = year,
                    artworkUrl = artwork?.ifBlank { null },
                    confidence = ConfidenceLevel.HIGH,
                    provider = "audd",
                    rawMetadata = mapOf("title" to title, "artist" to artist)
                )
                RecognitionResult(
                    status = RecognitionStatus.SUCCESS,
                    matches = listOf(match),
                    confidence = ConfidenceLevel.HIGH
                )
            }
        } catch (e: Exception) {
            RecognitionResult(RecognitionStatus.ERROR, errorMessage = e.message)
        }
    }

    override suspend fun recognizeFromSamples(samples: ByteArray, sampleRate: Int): RecognitionResult =
        withContext(Dispatchers.IO) {
            if (!ready) return@withContext RecognitionResult(RecognitionStatus.UNAVAILABLE, errorMessage = "No token")
            val tmp = File(context.cacheDir, "pcm_${System.currentTimeMillis()}.raw")
            try {
                tmp.writeBytes(samples)
                postSample(tmp)
            } finally {
                tmp.delete()
            }
        }

    override suspend fun startMicrophoneRecognition(): RecognitionSession? = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) return@withContext null
        if (!ready) return@withContext null
        micSession = MicSession()
        micSession
    }

    override suspend fun stopRecognition() {
        micSession?.stop()
        micSession = null
    }

    private inner class MicSession : RecognitionSession {
        private var recorder: AudioRecord? = null
        private var recognizing = false
        private var listener: RecognitionListener? = null

        override fun setListener(listener: RecognitionListener) {
            this.listener = listener
        }

        override suspend fun start() = withContext(Dispatchers.IO) {
            val sampleRate = 44100
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2
            )
            if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
                listener?.onRecognitionError("Microphone unavailable")
                return@withContext
            }
            recognizing = true
            listener?.onRecognitionStarted()
            recorder?.startRecording()
            val seconds = 8
            val bytesNeeded = sampleRate * 2 * seconds
            val buffer = ByteArray(bytesNeeded)
            var offset = 0
            while (offset < bytesNeeded && recognizing) {
                val read = recorder?.read(buffer, offset, buffer.size - offset) ?: -1
                if (read <= 0) break
                offset += read
            }
            stopRecorder()
            if (offset > sampleRate) {
                val result = recognizeFromSamples(buffer.copyOf(offset), sampleRate)
                val match = result.matches.firstOrNull()
                if (match != null && match.isUsableTitle()) listener?.onMatchFound(match)
                else listener?.onRecognitionError(result.errorMessage ?: "No match")
            }
            recognizing = false
            listener?.onRecognitionStopped()
        }

        override suspend fun stop() {
            recognizing = false
            stopRecorder()
            listener?.onRecognitionStopped()
        }

        override suspend fun isRecognizing(): Boolean = recognizing

        private fun stopRecorder() {
            try {
                recorder?.stop()
                recorder?.release()
            } catch (_: Exception) {
            }
            recorder = null
        }
    }
}
