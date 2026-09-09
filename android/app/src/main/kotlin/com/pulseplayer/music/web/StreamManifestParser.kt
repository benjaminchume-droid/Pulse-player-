package com.pulseplayer.music.web

import com.google.gson.JsonObject

class StreamManifestParser {
    fun parseManifest(json: JsonObject): List<String> {
        // Generic parser for various stream formats
        val urls = mutableListOf<String>()
        
        // For YouTube
        json.getAsJsonObject("streamingData")?.getAsJsonArray("adaptiveFormats")?.forEach { element ->
            element.asJsonObject.get("url")?.asString?.let { urls.add(it) }
        }
        
        // For Spotify embed
        json.getAsJsonObject("props")?.getAsJsonObject("pageProps")?.getAsJsonObject("state")
            ?.getAsJsonObject("data")?.getAsJsonObject("track")?.get("preview_url")?.asString
            ?.let { urls.add(it) }
        
        return urls
    }
}
