package com.pulseplayer.music.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

data class UpdateInfo(
    val tagName: String,
    val versionName: String,
    val releaseNotes: String,
    val apkUrl: String,
    val publishedAt: String
)

enum class UpdateState {
    IDLE, CHECKING, UP_TO_DATE, AVAILABLE, DOWNLOADING, PAUSED, READY_TO_INSTALL, ERROR
}

data class DownloadProgress(
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val percent: Int = 0,
    val speedBytesPerSec: Long = 0L,
    val state: UpdateState = UpdateState.IDLE,
    val message: String = "",
    val updateInfo: UpdateInfo? = null
) {
    val speedLabel: String
        get() = when {
            speedBytesPerSec >= 1_000_000 -> String.format("%.1f MB/s", speedBytesPerSec / 1_000_000.0)
            speedBytesPerSec >= 1_000 -> String.format("%.0f KB/s", speedBytesPerSec / 1_000.0)
            speedBytesPerSec > 0 -> "$speedBytesPerSec B/s"
            else -> "—"
        }
}

class UpdateChecker(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _progress = MutableStateFlow(DownloadProgress())
    val progress: StateFlow<DownloadProgress> = _progress.asStateFlow()

    private val paused = AtomicBoolean(false)
    private val cancelled = AtomicBoolean(false)
    private var downloadJob: Job? = null

    // Public GitHub API works for public repos; for private use a token if needed
    private val releasesApi =
        "https://api.github.com/repos/benjaminchume-droid/Pulse-player-/releases/latest"

    private val currentVersion: String
        get() = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }

    fun checkForUpdate() {
        if (_progress.value.state == UpdateState.DOWNLOADING) return
        scope.launch {
            _progress.value = DownloadProgress(state = UpdateState.CHECKING, message = "Checking GitHub releases…")
            try {
                val conn = (URL(releasesApi).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "PulsePlayer-Android")
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val json = JSONObject(body)
                val tag = json.optString("tag_name", "")
                val name = json.optString("name", tag)
                val notes = json.optString("body", "")
                val published = json.optString("published_at", "")
                var apkUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        val an = a.optString("name", "")
                        if (an.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = a.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                val remoteVersion = tag.removePrefix("v").trim()
                val local = currentVersion.trim()
                if (apkUrl.isBlank()) {
                    _progress.value = DownloadProgress(
                        state = UpdateState.ERROR,
                        message = "No APK asset found on latest release"
                    )
                    return@launch
                }
                if (compareVersions(remoteVersion, local) <= 0) {
                    _progress.value = DownloadProgress(
                        state = UpdateState.UP_TO_DATE,
                        message = "You're on the latest version ($local)"
                    )
                } else {
                    val info = UpdateInfo(tag, remoteVersion, notes, apkUrl, published)
                    _progress.value = DownloadProgress(
                        state = UpdateState.AVAILABLE,
                        message = "Update $remoteVersion available",
                        updateInfo = info
                    )
                }
            } catch (e: Exception) {
                _progress.value = DownloadProgress(
                    state = UpdateState.ERROR,
                    message = "Check failed: ${e.message ?: "network error"}"
                )
            }
        }
    }

    fun startDownload() {
        val info = _progress.value.updateInfo ?: return
        if (_progress.value.state == UpdateState.DOWNLOADING) return
        paused.set(false)
        cancelled.set(false)
        downloadJob?.cancel()
        downloadJob = scope.launch {
            downloadApk(info)
        }
    }

    fun pause() {
        if (_progress.value.state == UpdateState.DOWNLOADING) {
            paused.set(true)
            _progress.value = _progress.value.copy(state = UpdateState.PAUSED, message = "Paused")
        }
    }

    fun resume() {
        if (_progress.value.state == UpdateState.PAUSED) {
            paused.set(false)
            val info = _progress.value.updateInfo ?: return
            downloadJob?.cancel()
            downloadJob = scope.launch { downloadApk(info, resume = true) }
        }
    }

    fun cancel() {
        cancelled.set(true)
        paused.set(false)
        downloadJob?.cancel()
        _progress.value = DownloadProgress(
            state = if (_progress.value.updateInfo != null) UpdateState.AVAILABLE else UpdateState.IDLE,
            message = "Download cancelled",
            updateInfo = _progress.value.updateInfo
        )
    }

    private suspend fun downloadApk(info: UpdateInfo, resume: Boolean = false) {
        val outFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "pulse-player-update.apk")
        var downloaded = if (resume && outFile.exists()) outFile.length() else 0L

        try {
            val url = URL(info.apkUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 20000
                readTimeout = 30000
                setRequestProperty("User-Agent", "PulsePlayer-Android")
                if (downloaded > 0) setRequestProperty("Range", "bytes=$downloaded-")
            }
            val total = when {
                conn.getHeaderField("Content-Range") != null -> {
                    val range = conn.getHeaderField("Content-Range") // bytes x-y/total
                    range.substringAfter("/").toLongOrNull() ?: (downloaded + conn.contentLengthLong.coerceAtLeast(0))
                }
                conn.contentLengthLong > 0 -> if (downloaded > 0) downloaded + conn.contentLengthLong else conn.contentLengthLong
                else -> -1L
            }

            _progress.value = DownloadProgress(
                bytesDownloaded = downloaded,
                totalBytes = total.coerceAtLeast(0),
                percent = if (total > 0) ((downloaded * 100) / total).toInt() else 0,
                state = UpdateState.DOWNLOADING,
                message = "Downloading…",
                updateInfo = info
            )

            val input = BufferedInputStream(conn.inputStream)
            val output = FileOutputStream(outFile, resume && downloaded > 0)
            val buffer = ByteArray(64 * 1024)
            var lastTime = System.currentTimeMillis()
            var lastBytes = downloaded
            var read: Int

            while (input.read(buffer).also { read = it } != -1) {
                if (cancelled.get()) break
                while (paused.get() && !cancelled.get()) {
                    delay(200)
                }
                if (cancelled.get()) break

                output.write(buffer, 0, read)
                downloaded += read

                val now = System.currentTimeMillis()
                val dt = (now - lastTime).coerceAtLeast(1)
                if (dt >= 400) {
                    val speed = ((downloaded - lastBytes) * 1000L) / dt
                    val pct = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else 0
                    _progress.value = DownloadProgress(
                        bytesDownloaded = downloaded,
                        totalBytes = total.coerceAtLeast(0),
                        percent = pct,
                        speedBytesPerSec = speed,
                        state = UpdateState.DOWNLOADING,
                        message = "Downloading… $pct%",
                        updateInfo = info
                    )
                    lastTime = now
                    lastBytes = downloaded
                }
            }
            output.flush()
            output.close()
            input.close()
            conn.disconnect()

            if (cancelled.get()) {
                outFile.delete()
                return
            }

            _progress.value = DownloadProgress(
                bytesDownloaded = downloaded,
                totalBytes = total.coerceAtLeast(downloaded),
                percent = 100,
                speedBytesPerSec = 0,
                state = UpdateState.READY_TO_INSTALL,
                message = "Download complete — tap Install",
                updateInfo = info
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _progress.value = DownloadProgress(
                state = UpdateState.ERROR,
                message = "Download error: ${e.message}",
                updateInfo = info
            )
        }
    }

    fun installDownloadedApk() {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "pulse-player-update.apk")
        if (!file.exists()) {
            _progress.value = _progress.value.copy(state = UpdateState.ERROR, message = "APK file missing")
            return
        }
        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                Uri.fromFile(file)
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _progress.value = _progress.value.copy(
                state = UpdateState.ERROR,
                message = "Install failed: ${e.message}"
            )
        }
    }

    fun currentVersionName(): String = currentVersion

    private fun compareVersions(a: String, b: String): Int {
        val pa = a.split(".", "-").mapNotNull { it.toIntOrNull() }
        val pb = b.split(".", "-").mapNotNull { it.toIntOrNull() }
        val n = maxOf(pa.size, pb.size)
        for (i in 0 until n) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }

    fun dispose() {
        cancelled.set(true)
        scope.cancel()
    }
}
