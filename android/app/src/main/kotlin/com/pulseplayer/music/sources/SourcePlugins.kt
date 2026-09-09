package com.pulseplayer.music.sources

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Legal streaming + open-in-client sources only.
 * Piracy scrapers (NetNaija, etc.) are intentionally NOT implemented.
 */

data class StreamResult(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val streamUrl: String = "",
    val coverUrl: String = "",
    val externalUrl: String = "",
    val source: String,
    val canStreamInApp: Boolean
)

enum class SourceKind { STREAMING, OPEN_EXTERNAL, DOWNLOAD_URL }

interface MusicSourcePlugin {
    val id: String
    val displayName: String
    val kind: SourceKind
    val description: String
    suspend fun search(query: String, limit: Int = 20): List<StreamResult>
}

object InternetArchivePlugin : MusicSourcePlugin {
    override val id = "archive"
    override val displayName = "Internet Archive"
    override val kind = SourceKind.STREAMING
    override val description = "Public domain & freely licensed audio"

    override suspend fun search(query: String, limit: Int): List<StreamResult> = withContext(Dispatchers.IO) {
        try {
            val q = URLEncoder.encode("mediatype:audio $query", "UTF-8")
            val url = URL("https://archive.org/advancedsearch.php?q=$q&fl[]=identifier,title,creator,year&rows=$limit&page=1&output=json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("User-Agent", "PulsePlayer/1.0.3")
            }
            if (conn.responseCode !in 200..299) return@withContext emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val docs = JSONObject(body).optJSONObject("response")?.optJSONArray("docs") ?: return@withContext emptyList()
            buildList {
                for (i in 0 until docs.length()) {
                    val d = docs.getJSONObject(i)
                    val id = d.optString("identifier")
                    if (id.isBlank()) continue
                    val title = d.optString("title", id)
                    val artist = when (val c = d.opt("creator")) {
                        is String -> c
                        else -> d.optJSONArray("creator")?.optString(0) ?: "Internet Archive"
                    }
                    add(
                        StreamResult(
                            id = id,
                            title = title,
                            artist = artist,
                            streamUrl = "https://archive.org/download/$id/$id.mp3",
                            coverUrl = "https://archive.org/services/img/$id",
                            externalUrl = "https://archive.org/details/$id",
                            source = displayName,
                            canStreamInApp = true
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

object JamendoPlugin : MusicSourcePlugin {
    override val id = "jamendo"
    override val displayName = "Jamendo"
    override val kind = SourceKind.STREAMING
    override val description = "Creative Commons licensed music"

    // Public client id for demo; replace with your own for production quota
    private const val CLIENT_ID = "b6747d04"

    override suspend fun search(query: String, limit: Int): List<StreamResult> = withContext(Dispatchers.IO) {
        try {
            val q = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://api.jamendo.com/v3.0/tracks/?client_id=$CLIENT_ID&format=json&limit=$limit&search=$q&include=musicinfo")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
            }
            if (conn.responseCode !in 200..299) return@withContext emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val results = JSONObject(body).optJSONArray("results") ?: return@withContext emptyList()
            buildList {
                for (i in 0 until results.length()) {
                    val t = results.getJSONObject(i)
                    add(
                        StreamResult(
                            id = t.optString("id"),
                            title = t.optString("name"),
                            artist = t.optString("artist_name"),
                            album = t.optString("album_name"),
                            durationMs = t.optLong("duration") * 1000L,
                            streamUrl = t.optString("audio").ifBlank { t.optString("audiodownload") },
                            coverUrl = t.optString("album_image").ifBlank { t.optString("image") },
                            externalUrl = t.optString("shareurl"),
                            source = displayName,
                            canStreamInApp = true
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

object YouTubeMusicOpenPlugin : MusicSourcePlugin {
    override val id = "ytmusic"
    override val displayName = "YouTube Music"
    override val kind = SourceKind.OPEN_EXTERNAL
    override val description = "Search → open in YouTube Music (no in-app DRM stream)"

    override suspend fun search(query: String, limit: Int): List<StreamResult> = withContext(Dispatchers.IO) {
        // Metadata-only pathway: results open externally. No stream URL extraction.
        val q = URLEncoder.encode(query, "UTF-8")
        listOf(
            StreamResult(
                id = "ytm_search_$q",
                title = "Search YouTube Music: $query",
                artist = "Opens in YouTube Music",
                externalUrl = "https://music.youtube.com/search?q=$q",
                source = displayName,
                canStreamInApp = false
            ),
            StreamResult(
                id = "yt_search_$q",
                title = "Search YouTube: $query",
                artist = "Opens in YouTube",
                externalUrl = "https://www.youtube.com/results?search_query=$q",
                source = "YouTube",
                canStreamInApp = false
            )
        )
    }
}

object DirectUrlPlugin : MusicSourcePlugin {
    override val id = "direct"
    override val displayName = "Direct URL"
    override val kind = SourceKind.DOWNLOAD_URL
    override val description = "Paste a direct HTTPS audio link to stream or save"

    override suspend fun search(query: String, limit: Int): List<StreamResult> {
        val trimmed = query.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return emptyList()
        val name = trimmed.substringAfterLast('/').substringBefore('?').ifBlank { "Stream" }
        return listOf(
            StreamResult(
                id = trimmed.hashCode().toString(),
                title = name,
                artist = "Direct URL",
                streamUrl = trimmed,
                externalUrl = trimmed,
                source = displayName,
                canStreamInApp = true
            )
        )
    }
}

object SourceRegistry {
    val all: List<MusicSourcePlugin> = listOf(
        InternetArchivePlugin,
        JamendoPlugin,
        YouTubeMusicOpenPlugin,
        DirectUrlPlugin
    )

    val streaming get() = all.filter { it.kind == SourceKind.STREAMING || it.kind == SourceKind.DOWNLOAD_URL }
    val openExternal get() = all.filter { it.kind == SourceKind.OPEN_EXTERNAL }

    fun openExternal(context: Context, url: String) {
        try {
            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
        } catch (_: Exception) {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
