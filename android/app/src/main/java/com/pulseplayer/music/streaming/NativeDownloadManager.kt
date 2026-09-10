package com.pulseplayer.music.download

import com.pulseplayer.music.domain.StreamResolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext

class NativeDownloadManager(private val client: OkHttpClient = OkHttpClient()) {
    suspend fun download(resolution: StreamResolution, destination: File, onProgress: (Int) -> Unit = {}): File = withContext(Dispatchers.IO) {
        require(!resolution.expired()) { "Stream URL has expired; resolve again" }
        destination.parentFile?.mkdirs()
        val partial = File(destination.parentFile, destination.name + ".part")
        val headers = Headers.Builder().apply { resolution.headers.forEach { (k,v) -> add(k,v) } }.build()
        val request = Request.Builder().url(resolution.url).headers(headers).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body ?: error("Empty response")
            val mime = response.header("Content-Type")?.substringBefore(';')
            if (mime != null && !mime.startsWith("audio/") && !mime.startsWith("video/")) error("Refusing non-media response: $mime")
            val total = response.header("Content-Length")?.toLongOrNull()
            var written = 0L
            FileOutputStream(partial).use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n); written += n
                        if (total != null && total > 0) onProgress((written * 100 / total).toInt().coerceIn(0,100))
                    }
                }
                out.fd.sync()
            }
            if (total != null && written != total) error("Incomplete media transfer: $written/$total")
        }
        if (destination.exists()) destination.delete()
        check(partial.renameTo(destination)) { "Could not finalize download" }
        onProgress(100)
        destination
    }
}
