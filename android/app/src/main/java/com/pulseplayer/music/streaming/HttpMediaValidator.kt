package com.pulseplayer.music.network

import okhttp3.OkHttpClient
import okhttp3.Request

class HttpMediaValidator(private val client: OkHttpClient = OkHttpClient()) {
    fun validate(url: String): Boolean = runCatching {
        require(url.startsWith("https://", true)) { "HTTPS required" }
        client.newCall(Request.Builder().url(url).head().build()).execute().use { response ->
            if (!response.isSuccessful) return false
            val mime = response.header("Content-Type")?.substringBefore(';')
            mime == null || mime.startsWith("audio/") || mime.startsWith("video/")
        }
    }.getOrDefault(false)
}
