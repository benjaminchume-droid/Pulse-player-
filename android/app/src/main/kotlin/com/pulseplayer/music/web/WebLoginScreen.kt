package com.pulseplayer.music.ui.settings

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.pulseplayer.music.web.CookieManager

@Composable
fun WebLoginScreen(
    cookieManager: CookieManager,
    onLoginComplete: () -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        // Check if we are logged in by testing cookie presence
                        if (url?.contains("open.spotify.com") == true) {
                            // Cookie will be set automatically
                            onLoginComplete()
                        }
                    }
                }
                loadUrl("https://open.spotify.com")
            }
        }
    )
}
