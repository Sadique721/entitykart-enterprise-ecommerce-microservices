package com.example.entitykart

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * EntityKart Android App — v1.5.0
 *
 * WebView-based wrapper that loads the AngularJS frontend from local assets.
 * The backend API base URL is injected via JavascriptInterface so the JS layer
 * can call the correct Render cloud gateway without hardcoding any LAN IP.
 *
 * Production:   https://entitykart.onrender.com
 * Local Dev:    Uncomment the http://192.168.1.x line below and rebuild
 * AVD Emulator: Use http://10.0.2.2:9080 for host machine localhost
 */
class MainActivity : ComponentActivity() {

    // ── Production Render Cloud Deployment ────────────────────────────────────
    private val BACKEND_API_BASE = "https://entitykart.onrender.com"
    // ── Local Dev (uncomment for local Wi-Fi testing, rebuild APK after): ─────
    // private val BACKEND_API_BASE = "http://192.168.1.6:9080"
    // ── Android Emulator (AVD): ───────────────────────────────────────────────
    // private val BACKEND_API_BASE = "http://10.0.2.2:9080"
    // ─────────────────────────────────────────────────────────────────────────

    /** JavascriptInterface exposed to the web layer as `window.AndroidBridge` */
    inner class EntityKartBridge {
        @JavascriptInterface
        fun getApiBase(): String = BACKEND_API_BASE

        @JavascriptInterface
        fun getAppVersion(): String = "1.5.0"

        @JavascriptInterface
        fun isRenderDeploy(): Boolean = BACKEND_API_BASE.contains("onrender.com")
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        // ── Hardened WebView Settings ────────────────────────
                        settings.apply {
                            javaScriptEnabled = true          // Required for AngularJS SPA
                            domStorageEnabled = true          // Required for localStorage (JWT tokens)
                            allowFileAccess = false           // Prevent local filesystem access
                            allowContentAccess = false        // Prevent content provider access
                            @Suppress("DEPRECATION")
                            databaseEnabled = false           // Web SQL deprecated & insecure
                            // Allow HTTPS pages to load mixed content (needed for Render + CDN assets)
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            setSupportZoom(false)
                            builtInZoomControls = false
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            // Responsive design: honour <meta name="viewport"> tags
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            // App-specific user agent for API/CDN debugging
                            userAgentString = "${userAgentString} EntityKartApp/1.5.0"
                        }

                        // ── Expose native bridge to JavaScript ────────────────
                        // Accessible in JS as: window.AndroidBridge.getApiBase()
                        addJavascriptInterface(EntityKartBridge(), "AndroidBridge")

                        // ── Navigation guard and API injection ────────────────
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                val url = request.url.toString()
                                // Whitelist: local assets, Render HTTPS backend, public CDNs
                                val allowedPrefixes = listOf(
                                    "file:///android_asset/",
                                    "https://entitykart.onrender.com",
                                    "https://fonts.googleapis.com",
                                    "https://fonts.gstatic.com",
                                    "https://cdnjs.cloudflare.com",
                                    "https://ajax.googleapis.com",
                                    "https://cdn.jsdelivr.net",
                                    "https://res.cloudinary.com"
                                )
                                val isAllowed = allowedPrefixes.any { url.startsWith(it) }
                                return !isAllowed // true = block navigation, false = allow
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                super.onPageFinished(view, url)
                                // Inject Render API base into window scope AND clear any stale
                                // locally-saved LAN IP so AngularJS routes to the cloud backend
                                view.evaluateJavascript(
                                    """
                                    (function() {
                                        window.ENTITYKART_API_BASE = '${BACKEND_API_BASE}';
                                        try {
                                            localStorage.setItem('RENDER_DEPLOY', 'true');
                                            // Clear stale local LAN IPs so app.js picks up Render URL
                                            var savedIp = localStorage.getItem('API_IP');
                                            if (!savedIp || savedIp === '192.168.1.6' ||
                                                savedIp === 'localhost' || savedIp === '127.0.0.1' ||
                                                savedIp === '10.0.2.2') {
                                                localStorage.removeItem('API_IP');
                                                localStorage.removeItem('API_PORT');
                                            }
                                        } catch(e) {}
                                    })();
                                    """.trimIndent(),
                                    null
                                )
                            }
                        }

                        loadUrl("file:///android_asset/index.html")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
