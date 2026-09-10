package com.pulseplayer.music.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class LyricsResult(
    val plain: String,
    val synced: String,
    val source: String
)

/**
 * Multi-provider lyrics: LRCLIB primary, Lyrics.ovh fallback.
 * Never invents lyrics; returns null on failure.
 */
object LyricsRepository {

    suspend fun fetchLyrics(
        title: String,
        artist: String,
        album: String = "",
        durationSec: Int = 0
    ): LyricsResult? = withContext(Dispatchers.IO) {
        if (title.isBlank() || title.all { it.isDigit() }) return@withContext null
        fetchLrclib(title, artist, album) ?: fetchLyricsOvh(title, artist)
    }

    private fun fetchLrclib(title: String, artist: String, album: String): LyricsResult? {
        return try {
            val qTitle = URLEncoder.encode(title.trim(), "UTF-8")
            val qArtist = URLEncoder.encode(artist.trim(), "UTF-8")
            val qAlbum = URLEncoder.encode(album.trim(), "UTF-8")
            val urlStr = buildString {
                append("https://lrclib.net/api/search?track_name=$qTitle&artist_name=$qArtist")
                if (album.isNotBlank()) append("&album_name=$qAlbum")
            }
            val body = httpGet(urlStr, "PulsePlayer/1.0.4") ?: return null
            val arr = JSONArray(body)
            if (arr.length() == 0) return null
            var best: JSONObject? = null
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (!o.optString("syncedLyrics").isNullOrBlank()) {
                    best = o
                    break
                }
                if (best == null) best = o
            }
            val obj = best ?: return null
            val plain = obj.optString("plainLyrics", "").ifBlank {
                obj.optString("syncedLyrics", "")
                    .lines()
                    .map { it.replace(Regex("\\[\\d:.]+\\]"), "").trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString("\n")
            }
            val synced = obj.optString("syncedLyrics", "")
            if (plain.isBlank() && synced.isBlank()) return null
            LyricsResult(plain, synced, "LRCLIB")
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchLyricsOvh(title: String, artist: String): LyricsResult? {
        return try {
            val a = URLEncoder.encode(artist.trim(), "UTF-8")
            val t = URLEncoder.encode(title.trim(), "UTF-8")
            val body = httpGet("https://api.lyrics.ovh/v1/$a/$t", "PulsePlayer/1.0.4") ?: return null
            val lyrics = JSONObject(body).optString("lyrics", "").trim()
            if (lyrics.isBlank()) null else LyricsResult(lyrics, "", "lyrics.ovh")
        } catch (_: Exception) {
            null
        }
    }

    private fun httpGet(urlStr: String, userAgent: String): String? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode !in 200..299) null
            else conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

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
            if (text.isNotEmpty()) result.add(min * 60_000 + sec * 1_000 + ms to text)
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
