package com.pulseplayer.music.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class LyricsResult(
    val plain: String,
    val synced: String, // LRC
    val source: String
)

object LyricsRepository {

    /**
     * Fetches lyrics from LRCLIB (free, no key required for basic search).
     * https://lrclib.net/docs
     */
    suspend fun fetchLyrics(
        title: String,
        artist: String,
        album: String = "",
        durationSec: Int = 0
    ): LyricsResult? = withContext(Dispatchers.IO) {
        try {
            val qTitle = URLEncoder.encode(title.trim(), "UTF-8")
            val qArtist = URLEncoder.encode(artist.trim(), "UTF-8")
            val qAlbum = URLEncoder.encode(album.trim(), "UTF-8")
            val urlStr = buildString {
                append("https://lrclib.net/api/search?track_name=$qTitle&artist_name=$qArtist")
                if (album.isNotBlank()) append("&album_name=$qAlbum")
            }
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("User-Agent", "PulsePlayer/1.0.2")
            }
            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return@withContext null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val arr = org.json.JSONArray(body)
            if (arr.length() == 0) return@withContext null

            // Prefer exact-ish match with synced lyrics
            var best: JSONObject? = null
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val hasSynced = !o.optString("syncedLyrics").isNullOrBlank()
                if (hasSynced) {
                    best = o
                    break
                }
                if (best == null) best = o
            }
            val obj = best ?: return@withContext null
            val plain = obj.optString("plainLyrics", "").ifBlank {
                obj.optString("syncedLyrics", "")
                    .lines()
                    .map { it.replace(Regex("\\[\\d:.]+\\]"), "").trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString("\n")
            }
            val synced = obj.optString("syncedLyrics", "")
            if (plain.isBlank() && synced.isBlank()) return@withContext null
            LyricsResult(plain = plain, synced = synced, source = "LRCLIB")
        } catch (_: Exception) {
            null
        }
    }

    /** Parse LRC into (timeMs, line) pairs for synced display */
    fun parseLrc(lrc: String): List<Pair<Long, String>> {
        val result = mutableListOf<Pair<Long, String>>()
        val lineRegex = Regex("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\](.*)")
        lrc.lines().forEach { line ->
            val m = lineRegex.find(line.trim()) ?: return@forEach
            val min = m.groupValues[1].toLongOrNull() ?: return@forEach
            val sec = m.groupValues[2].toLongOrNull() ?: return@forEach
            val msPart = m.groupValues[3]
            val ms = when {
                msPart.isEmpty() -> 0L
                msPart.length == 1 -> msPart.toLong() * 100
                msPart.length == 2 -> msPart.toLong() * 10
                else -> msPart.take(3).toLong()
            }
            val text = m.groupValues[4].trim()
            if (text.isNotEmpty()) {
                result.add(min * 60_000 + sec * 1_000 + ms to text)
            }
        }
        return result.sortedBy { it.first }
    }

    fun activeLineIndex(parsed: List<Pair<Long, String>>, positionMs: Long): Int {
        if (parsed.isEmpty()) return -1
        var idx = 0
        for (i in parsed.indices) {
            if (parsed[i].first <= positionMs) idx = i else break
        }
        return idx
    }
}
