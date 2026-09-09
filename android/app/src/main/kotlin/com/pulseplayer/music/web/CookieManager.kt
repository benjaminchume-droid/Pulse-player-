package com.pulseplayer.music.web

import android.content.Context
import android.webkit.CookieManager as AndroidCookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Cookie
import okhttp3.HttpUrl
import kotlin.coroutines.resume

class CookieManager(context: Context) {
    private val webView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                // Cookie will be set automatically
            }
        }
    }
    private val androidCookieManager = AndroidCookieManager.getInstance()
    
    suspend fun authenticateSpotify(): Boolean = suspendCancellableCoroutine { cont ->
        webView.loadUrl("https://open.spotify.com")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (url?.contains("open.spotify.com") == true) {
                    cont.resume(true)
                }
            }
        }
    }
    
    suspend fun authenticateYouTube(): Boolean = suspendCancellableCoroutine { cont ->
        webView.loadUrl("https://music.youtube.com")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (url?.contains("music.youtube.com") == true) {
                    cont.resume(true)
                }
            }
        }
    }
    
    fun getCookies(url: String): String? {
        return androidCookieManager.getCookie(url)
    }
}
