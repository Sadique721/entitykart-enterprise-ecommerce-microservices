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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL

/**
 * EntityKart Android App — v2.0.0
 *
 * DYNAMIC NETWORK DETECTION:
 * - Same Wi-Fi as PC  →  auto-detects local LAN IP on port 9900 (Docker gateway)
 * - Different network →  falls back to Render cloud automatically
 *
 * No hardcoded IP required. The app scans the phone's subnet at launch.
 */
class MainActivity : ComponentActivity() {

    companion object {
        const val RENDER_URL      = "https://entitykart-enterprise-ecommerce.onrender.com"
        const val GATEWAY_PORT    = 9900
        const val PROBE_TIMEOUT   = 350        // ms per host probe
        const val PROBE_PATH      = "/actuator/info"
    }

    // Holds the resolved backend URL — updated after subnet scan completes
    @Volatile
    private var resolvedBase: String = ""

    /** Bridge exposed as window.AndroidBridge in JavaScript */
    inner class EntityKartBridge {
        @JavascriptInterface
        fun getApiBase(): String = if (resolvedBase.isEmpty()) RENDER_URL else resolvedBase

        @JavascriptInterface
        fun getAppVersion(): String = "2.0.0"

        @JavascriptInterface
        fun isLocalNetwork(): Boolean = resolvedBase.startsWith("http://")
    }

    // ─────────────────────────────────────────────────────────────────────────
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val activity = this   // explicit ref so lambdas below can access Activity members

        setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val webView = WebView(ctx)

                    // ── WebView settings ──────────────────────────────────────
                    webView.settings.apply {
                        javaScriptEnabled          = true
                        domStorageEnabled           = true
                        allowFileAccess             = false
                        allowContentAccess          = false
                        mixedContentMode            = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        setSupportZoom(false)
                        builtInZoomControls         = false
                        displayZoomControls         = false
                        cacheMode                   = WebSettings.LOAD_DEFAULT
                        useWideViewPort             = true
                        loadWithOverviewMode        = true
                        userAgentString             = "${userAgentString} EntityKartApp/2.0.0"
                    }

                    // ── Expose bridge to JS as window.AndroidBridge ───────────
                    webView.addJavascriptInterface(activity.EntityKartBridge(), "AndroidBridge")

                    // ── Navigation guard ──────────────────────────────────────
                    webView.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean = false  // allow all; cleartext controlled by network_security_config

                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            activity.injectApiBase(view)
                        }
                    }

                    // ── Load assets immediately (before discovery completes) ──
                    webView.loadUrl("file:///android_asset/index.html")

                    // ── Discover backend in background ────────────────────────
                    CoroutineScope(Dispatchers.IO).launch {
                        val found = activity.discoverBackend()
                        activity.resolvedBase = found
                        withContext(Dispatchers.Main) {
                            activity.injectApiBase(webView)
                        }
                    }

                    webView
                }
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inject the resolved base URL into the running WebView page
    // ─────────────────────────────────────────────────────────────────────────
    private fun injectApiBase(view: WebView) {
        val base    = if (resolvedBase.isEmpty()) RENDER_URL else resolvedBase
        val isLocal = base.startsWith("http://")
        val js = """
            (function() {
                window.ENTITYKART_API_BASE = '$base';
                try {
                    if ($isLocal) {
                        localStorage.removeItem('RENDER_DEPLOY');
                        var parts = '$base'.replace('http://','').split(':');
                        localStorage.setItem('API_IP',   parts[0]);
                        localStorage.setItem('API_PORT', parts[1] || '9900');
                    } else {
                        localStorage.setItem('RENDER_DEPLOY', 'true');
                        localStorage.removeItem('API_IP');
                        localStorage.removeItem('API_PORT');
                    }
                } catch(e) {}
                console.log('[EntityKart] API_BASE = $base');
            })();
        """.trimIndent()
        view.evaluateJavascript(js, null)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scan phone's subnet for the gateway; return first working URL or Render
    // ─────────────────────────────────────────────────────────────────────────
    private suspend fun discoverBackend(): String = withContext(Dispatchers.IO) {
        val subnets = getLocalSubnets()
        if (subnets.isEmpty()) return@withContext RENDER_URL

        // Probe the most common host-PC IP suffixes first for speed
        val priorities = listOf(1,2,3,4,5,10,20,21,22,23,24,25,30,
            50,100,101,102,103,104,105,150,200,210,220,230,240,250)

        for (subnet in subnets) {
            for (host in priorities) {
                val ip = "$subnet.$host"
                if (probeGateway(ip)) {
                    return@withContext "http://$ip:$GATEWAY_PORT"
                }
            }
        }
        return@withContext RENDER_URL
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Returns /24 subnet prefixes for all active non-loopback network interfaces
    // Example: ["192.168.1", "10.0.0"]   Excludes Docker/WSL (172.x) ranges
    // ─────────────────────────────────────────────────────────────────────────
    private fun getLocalSubnets(): List<String> {
        val result = mutableListOf<String>()
        try {
            val ifaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: return result
            for (iface in ifaces) {
                if (!iface.isUp || iface.isLoopback) continue
                for (addr in iface.interfaceAddresses) {
                    val rawIp = addr.address ?: continue
                    if (rawIp.isLoopbackAddress) continue
                    val ip = rawIp.hostAddress ?: continue
                    if (!ip.contains('.')) continue          // skip IPv6
                    val parts = ip.split('.')
                    if (parts.size != 4) continue
                    val prefix = "${parts[0]}.${parts[1]}.${parts[2]}"
                    // Exclude Docker/WSL bridge (172.x) and link-local (169.254.x)
                    if (prefix.startsWith("172.") || prefix.startsWith("169.254")) continue
                    result.add(prefix)
                }
            }
        } catch (_: Exception) {}
        return result.distinct()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Probe whether the EntityKart gateway responds on http://{ip}:9900
    // Accepts HTTP 200, 401 (JWT filter), or 403 as "service is up" signals
    // ─────────────────────────────────────────────────────────────────────────
    private fun probeGateway(ip: String): Boolean {
        return try {
            val url  = URL("http://$ip:$GATEWAY_PORT$PROBE_PATH")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = PROBE_TIMEOUT
            conn.readTimeout    = PROBE_TIMEOUT
            conn.requestMethod  = "GET"
            conn.connect()
            val code = conn.responseCode
            conn.disconnect()
            code in 200..403
        } catch (_: Exception) {
            false
        }
    }
}
