package com.pulseplayer.music.metadata

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.pulseplayer.music.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class EnrichedMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val year: Int,
    val genre: String,
    val coverUrl: String,
    val trackNumber: Int = 0,
    val applied: Boolean = true
)

object MetadataEnricher {

    /** Never treat pure numbers / MediaStore-style IDs as song titles. */
    fun isValidTitle(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val t = value.trim()
        if (t.equals("Unknown", true) || t.equals("<unknown>", true)) return false
        if (t.all { it.isDigit() || it == '-' || it == '_' }) return false
        if (t.matches(Regex("^\\d{5,}$"))) return false
        if (t.matches(Regex("(?i)^track\\s*\\d+$"))) return false
        return true
    }

    fun isPlaceholderArtist(value: String?): Boolean {
        if (value.isNullOrBlank()) return true
        val a = value.trim()
        return a.equals("Unknown Artist", true) ||
            a.equals("<unknown>", true) ||
            a.equals("Unknown", true) ||
            a.all { it.isDigit() }
    }

    suspend fun enrich(context: Context, song: Song): EnrichedMetadata = withContext(Dispatchers.IO) {
        val fromFile = readEmbedded(context, song.path)

        var title = when {
            isValidTitle(fromFile.title) -> fromFile.title
            isValidTitle(song.title) -> song.title
            else -> cleanTitleFromFilename(song.path).ifBlank { song.title }
        }
        // Hard guard: never keep numeric titles
        if (!isValidTitle(title)) {
            title = cleanTitleFromFilename(song.path).ifBlank { "Unknown Track" }
        }

        var artist = when {
            !isPlaceholderArtist(fromFile.artist) -> fromFile.artist
            !isPlaceholderArtist(song.artist) -> song.artist
            else -> ""
        }
        var album = fromFile.album.ifBlank { song.album }.let {
            if (it.equals("Unknown Album", true) || it.all { c -> c.isDigit() }) "" else it
        }
        var year = fromFile.year.takeIf { it in 1900..2100 } ?: song.year.takeIf { it in 1900..2100 } ?: 0
        var genre = fromFile.genre.ifBlank { song.genre }
        var albumArtist = fromFile.albumArtist
        var coverUrl = song.coverUrl
        var trackNumber = fromFile.trackNumber

        // Only call MusicBrainz when we have a real title to search — never search by numeric id
        if (isValidTitle(title) && (artist.isBlank() || album.isBlank())) {
            val online = searchMusicBrainz(title, artist)
            if (online != null) {
                if (isValidTitle(online.title) && !isValidTitle(fromFile.title)) {
                    // only replace if embedded title was bad
                    if (!isValidTitle(song.title) || song.title.all { it.isDigit() }) {
                        title = online.title
                    }
                }
                if (artist.isBlank() && online.artist.isNotBlank()) artist = online.artist
                if (album.isBlank() && online.album.isNotBlank()) album = online.album
                if (albumArtist.isBlank()) albumArtist = online.albumArtist
                if (year == 0) year = online.year
                if (genre.isBlank()) genre = online.genre
                if (coverUrl.isBlank()) coverUrl = online.coverUrl
            }
        }

        EnrichedMetadata(
            title = if (isValidTitle(title)) title else song.title.ifBlank { "Unknown Track" }.let {
                if (isValidTitle(it)) it else "Unknown Track"
            },
            artist = artist.ifBlank { "Unknown Artist" },
            album = album.ifBlank { "Unknown Album" },
            albumArtist = albumArtist,
            year = year,
            genre = genre,
            coverUrl = coverUrl,
            trackNumber = trackNumber
        )
    }

    private fun readEmbedded(context: Context, path: String): EnrichedMetadata {
        if (path.isBlank()) return EnrichedMetadata("", "", "", "", 0, "", "")
        val r = MediaMetadataRetriever()
        return try {
            if (path.startsWith("content://") || path.startsWith("file://")) {
                r.setDataSource(context, Uri.parse(path))
            } else {
                r.setDataSource(path)
            }
            val title = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty()
            val artist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty()
            val album = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty()
            val albumArtist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST).orEmpty()
            val genre = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE).orEmpty()
            val yearStr = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR).orEmpty()
            val year = yearStr.take(4).toIntOrNull() ?: 0
            val track = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore("/")?.toIntOrNull() ?: 0
            EnrichedMetadata(title, artist, album, albumArtist, year, genre, "", track)
        } catch (_: Exception) {
            EnrichedMetadata("", "", "", "", 0, "", "")
        } finally {
            try { r.release() } catch (_: Exception) {}
        }
    }

    private fun cleanTitleFromFilename(path: String): String {
        if (path.isBlank()) return ""
        val name = path.substringAfterLast('/').substringBeforeLast('.')
            .substringAfterLast(':') // content uri tail
        val cleaned = name
            .replace(Regex("^\\d+[\\-_\\.\\s]+"), "")
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (isValidTitle(cleaned)) cleaned else ""
    }

    private fun searchMusicBrainz(title: String, artist: String): EnrichedMetadata? {
        if (!isValidTitle(title)) return null
        try {
            val query = buildString {
                append("recording:\"${title.replace("\"", "")}\"")
                if (artist.isNotBlank() && !isPlaceholderArtist(artist)) {
                    append(" AND artist:\"${artist.replace("\"", "")}\"")
                }
            }
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://musicbrainz.org/ws/2/recording/?query=$encoded&fmt=json&limit=1")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("User-Agent", "PulsePlayer/1.0.4 (https://github.com/benjaminchume-droid/Pulse-player-)")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val recordings = JSONObject(body).optJSONArray("recordings") ?: return null
            if (recordings.length() == 0) return null
            val rec = recordings.getJSONObject(0)
            val t = rec.optString("title", title)
            if (!isValidTitle(t)) return null
            var a = artist
            val credits = rec.optJSONArray("artist-credit")
            if (credits != null && credits.length() > 0) {
                a = credits.getJSONObject(0).optJSONObject("artist")?.optString("name") ?: a
            }
            var album = ""
            var year = 0
            val releases = rec.optJSONArray("releases")
            if (releases != null && releases.length() > 0) {
                val rel = releases.getJSONObject(0)
                album = rel.optString("title", "")
                year = rel.optString("date", "").take(4).toIntOrNull() ?: 0
            }
            return EnrichedMetadata(
                title = t,
                artist = a.ifBlank { "Unknown Artist" },
                album = album.ifBlank { "Unknown Album" },
                albumArtist = a,
                year = year,
                genre = "",
                coverUrl = ""
            )
        } catch (_: Exception) {
            return null
        }
    }
}
