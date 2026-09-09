package com.pulseplayer.music.download

import com.pulseplayer.music.web.SpotifyWebExtractor
import com.pulseplayer.music.web.YouTubeWebExtractor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class WebDownloadManager(
    private val spotify: SpotifyWebExtractor,
    private val youtube: YouTubeWebExtractor,
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun downloadTrack(source: String, id: String, outputFile: File): File {
        val streamUrl = when (source) {
            "spotify" -> spotify.extractStreamUrl(id)
            "youtube" -> youtube.extractStreamUrl(id)
            else -> error("Unsupported source")
        }
        
        // Download the stream
        val request = Request.Builder().url(streamUrl).build()
        val response = client.newCall(request).execute()
        response.body?.byteStream()?.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        } ?: error("Download failed")
        
        return outputFile
    }
}
