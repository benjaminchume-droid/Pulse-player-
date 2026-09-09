package com.pulseplayer.music.metadata

import android.content.Context
import android.media.MediaMetadataRetriever
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
    val trackNumber: Int = 0
)

object MetadataEnricher {

    /**
     * 1) Read embedded tags from the file via MediaMetadataRetriever
     * 2) If still weak (Unknown / empty), query MusicBrainz recording search
     */
    suspend fun enrich(context: Context, song: Song): EnrichedMetadata = withContext(Dispatchers.IO) {
        val fromFile = readEmbedded(song.path)
        var title = fromFile.title.ifBlank { cleanTitleFromFilename(song.path) }.ifBlank { song.title }
        var artist = fromFile.artist.ifBlank { song.artist }.let {
            if (it.equals("Unknown Artist", true) || it.equals("<unknown>", true)) "" else it
        }
        var album = fromFile.album.ifBlank { song.album }.let {
            if (it.equals("Unknown Album", true)) "" else it
        }
        var year = fromFile.year
        var genre = fromFile.genre.ifBlank { song.genre }
        var albumArtist = fromFile.albumArtist
        var coverUrl = song.coverUrl
        var trackNumber = fromFile.trackNumber

        // Online lookup when tags are thin
        if (artist.isBlank() || album.isBlank() || title.isBlank()) {
            val online = searchMusicBrainz(title, artist)
            if (online != null) {
                if (title.isBlank()) title = online.title
                if (artist.isBlank()) artist = online.artist
                if (album.isBlank()) album = online.album
                if (albumArtist.isBlank()) albumArtist = online.albumArtist
                if (year == 0) year = online.year
                if (genre.isBlank() || genre == "All Streams") genre = online.genre.ifBlank { genre }
                if (coverUrl.isBlank()) coverUrl = online.coverUrl
            }
        }

        // Cover from Cover Art Archive if we got an MB release later — keep simple for now
        EnrichedMetadata(
            title = title.ifBlank { song.title },
            artist = artist.ifBlank { "Unknown Artist" },
            album = album.ifBlank { "Unknown Album" },
            albumArtist = albumArtist,
            year = year,
            genre = genre.ifBlank { "All Streams" },
            coverUrl = coverUrl,
            trackNumber = trackNumber
        )
    }

    private fun readEmbedded(path: String): EnrichedMetadata {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(path)
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
        val name = path.substringAfterLast('/').substringBeforeLast('.')
        return name
            .replace(Regex("^\\d+[\-_\.\s]+"), "")
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun searchMusicBrainz(title: String, artist: String): EnrichedMetadata? {
        try {
            val query = buildString {
                if (title.isNotBlank()) append("recording:\"${title.replace("\"", "")}\"")
                if (artist.isNotBlank()) {
                    if (isNotEmpty()) append(" AND ")
                    append("artist:\"${artist.replace("\"", "")}\"")
                }
            }
            if (query.isBlank()) return null
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://musicbrainz.org/ws/2/recording/?query=$encoded&fmt=json&limit=1")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("User-Agent", "PulsePlayer/1.0.2 (https://github.com/benjaminchume-droid/Pulse-player-)")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val json = JSONObject(body)
            val recordings = json.optJSONArray("recordings") ?: return null
            if (recordings.length() == 0) return null
            val rec = recordings.getJSONObject(0)
            val t = rec.optString("title", title)
            var a = artist
            val credit = rec.optJSONObject("artist-credit")
            // artist-credit is array in MB JSON
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
                val date = rel.optString("date", "")
                year = date.take(4).toIntOrNull() ?: 0
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
