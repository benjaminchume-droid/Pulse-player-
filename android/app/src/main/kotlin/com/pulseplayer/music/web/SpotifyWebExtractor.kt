package com.pulseplayer.music.web

import android.webkit.CookieManager
import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder

class SpotifyWebExtractor(
    private val cookieManager: CookieManager,
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun extractStreamUrl(trackId: String, accessToken: String? = null): String {
        // Use the embed page to get the media URL without DRM
        val embedUrl = "https://open.spotify.com/embed/track/$trackId"
        val request = Request.Builder()
            .url(embedUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .addHeader("Cookie", cookieManager.getCookie(embedUrl) ?: "")
            .build()
        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: error("No HTML")
        
        // Extract __NEXT_DATA__ JSON
        val jsonStart = html.indexOf("__NEXT_DATA__") + 15
        val jsonEnd = html.indexOf("</script>", jsonStart)
        val jsonString = html.substring(jsonStart, jsonEnd).trim()
        val json = JsonParser.parseString(jsonString).asJsonObject
        
        // Navigate to the media URL
        val mediaUrl = json
            .getAsJsonObject("props")
            .getAsJsonObject("pageProps")
            .getAsJsonObject("state")
            .getAsJsonObject("data")
            .getAsJsonObject("track")
            .get("preview_url")?.asString
        
        return mediaUrl ?: error("No media URL found")
    }
    
    suspend fun getAuthenticatedAccessToken(): String? {
        // If we have a session cookie, we can request an access token from the internal API
        // Using the Spotify Web API with client credentials flow as fallback
        // For free tier, we can use the embed method; for premium, we could use the official SDK
        return null // Embed method works without token
    }
}
