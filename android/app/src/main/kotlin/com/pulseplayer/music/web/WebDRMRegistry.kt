package com.pulseplayer.music.integration

import com.pulseplayer.music.web.SpotifyWebExtractor
import com.pulseplayer.music.web.YouTubeWebExtractor
import com.pulseplayer.music.web.CookieManager
import com.pulseplayer.music.download.WebDownloadManager
import com.pulseplayer.music.omnisource.api.OmniSourceRegistry
import com.pulseplayer.music.omnisource.api.OmniTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WebDRMRegistry(
    private val registry: OmniSourceRegistry,
    private val cookieManager: CookieManager
) {
    suspend fun registerWebExtractors() {
        // Override Spotify and YouTube with web-based extraction
        registry.overrideSource("spotify", WebSpotifyAdapter(cookieManager))
        registry.overrideSource("youtube-music", WebYouTubeAdapter(cookieManager))
    }
}

class WebSpotifyAdapter(private val cookieManager: CookieManager) {
    private val extractor = SpotifyWebExtractor(cookieManager)
    
    suspend fun search(query: String): List<OmniTrack> {
        // Use existing search, but we'll extract stream on resolve
        return emptyList()
    }
    
    suspend fun resolve(track: OmniTrack): OmniTrack {
        val streamUrl = extractor.extractStreamUrl(track.id)
        return track.copy(streamUrl = streamUrl)
    }
}

class WebYouTubeAdapter(private val cookieManager: CookieManager) {
    private val extractor = YouTubeWebExtractor()
    
    suspend fun resolve(track: OmniTrack): OmniTrack {
        val streamUrl = extractor.extractStreamUrl(track.id)
        return track.copy(streamUrl = streamUrl)
    }
}
