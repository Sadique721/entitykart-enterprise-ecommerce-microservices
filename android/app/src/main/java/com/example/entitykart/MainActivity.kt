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
 * EntityKart Android App
 *
 * WebView-based wrapper that loads the AngularJS frontend from local assets.
 * The backend API base URL is injected via JavascriptInterface so the JS layer
 * can call the correct gateway IP/port without hardcoding.
 *
 * To point to a different backend: change BACKEND_API_BASE below.
 * For Android Emulator (AVD): 10.0.2.2 maps to host machine localhost.
 * For physical device on same Wi-Fi: use your PC's LAN IP e.g. 192.168.1.x
 */
class MainActivity : ComponentActivity() {

    // ── Change this to your actual API Gateway URL ───────────────────────────
    private val BACKEND_API_BASE = "http://192.168.1.6:9080"
    // ─────────────────────────────────────────────────────────────────────────

    /** JavascriptInterface exposed to the web layer as `window.AndroidBridge` */
    inner class EntityKartBridge {
        @JavascriptInterface
        fun getApiBase(): String = BACKEND_API_BASE

        @JavascriptInterface
        fun getAppVersion(): String = "1.0"
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
                            javaScriptEnabled = true          // Required for AngularJS
                            domStorageEnabled = true          // Required for localStorage (auth tokens)
                            allowFileAccess = false           // Prevent local filesystem access
                            allowContentAccess = false        // Prevent content provider access
                            @Suppress("DEPRECATION")
                            databaseEnabled = false           // Web SQL deprecated & insecure
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            setSupportZoom(false)
                            builtInZoomControls = false
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }

                        // ── Expose native bridge to JavaScript ────────────────
                        // Access in JS as: window.AndroidBridge.getApiBase()
                        // app.js reads window.ENTITYKART_API_BASE which is set by index.html init script
                        addJavascriptInterface(EntityKartBridge(), "AndroidBridge")

                        // ── Restrict navigation to local assets only ──────────
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                val url = request.url.toString()
                                // Allow file:// (local assets) and the configured API gateway
                                val allowedPrefixes = listOf(
                                    "file:///android_asset/",
                                    BACKEND_API_BASE
                                )
                                val isAllowed = allowedPrefixes.any { url.startsWith(it) }
                                return !isAllowed // return true = block, false = allow
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                super.onPageFinished(view, url)
                                // Inject API base URL into window scope so AngularJS can read it
                                view.evaluateJavascript(
                                    "window.ENTITYKART_API_BASE = '${BACKEND_API_BASE}';",
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
