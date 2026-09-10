package com.pulseplayer.music.sources

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Source plugins — no hardcoded commercial music site scrapers.
 * Users provide queries; plugins that need API keys stay disabled until configured.
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

/** Paste a direct HTTPS audio URL to stream. */
object DirectUrlPlugin : MusicSourcePlugin {
    override val id = "direct"
    override val displayName = "Direct URL"
    override val kind = SourceKind.DOWNLOAD_URL
    override val description = "Paste a direct HTTPS audio link"

    override suspend fun search(query: String, limit: Int): List<StreamResult> {
        val trimmed = query.trim()
        if (!trimmed.startsWith("https://")) return emptyList()
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

/** Opens a web search in the browser — does not scrape or hardcode stream hosts. */
object OpenSearchPlugin : MusicSourcePlugin {
    override val id = "web_search"
    override val displayName = "Web search"
    override val kind = SourceKind.OPEN_EXTERNAL
    override val description = "Open search in your browser"

    override suspend fun search(query: String, limit: Int): List<StreamResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val q = java.net.URLEncoder.encode(query, "UTF-8")
        listOf(
            StreamResult(
                id = "web_$q",
                title = "Search: $query",
                artist = "Opens in browser",
                externalUrl = "https://duckduckgo.com/?q=$q",
                source = displayName,
                canStreamInApp = false
            )
        )
    }
}

object SourceRegistry {
    val all: List<MusicSourcePlugin> = listOf(DirectUrlPlugin, OpenSearchPlugin)

    fun openExternal(context: Context, url: String) {
        try {
            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
        } catch (_: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
