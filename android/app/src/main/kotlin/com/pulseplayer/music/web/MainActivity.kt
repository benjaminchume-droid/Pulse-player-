// Initialize web-based DRM
val cookieManager = CookieManager(this)
val webRegistry = WebDRMRegistry(OmniSourceRegistry(this), cookieManager)
lifecycleScope.launch {
    webRegistry.registerWebExtractors()
    // Optionally, pre-authenticate if we have saved cookies
}
