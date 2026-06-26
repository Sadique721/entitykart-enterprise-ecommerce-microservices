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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.NetworkInterface
import java.net.URL

/**
 * EntityKart Android App — v3.0.0
 *
 * DYNAMIC NETWORK DETECTION (Local-Only Mode):
 * - Shows loading screen while scanning the phone's subnet
 * - Finds the PC running Docker (port 9900) automatically
 * - Works on ANY WiFi — Pardeep, Sadique 2, hotspot, office, home
 * - Retries scan every 10 seconds until gateway is found
 * - NO Render/cloud fallback — pure local network
 *
 * Network logic:
 *   1. Get phone's own WiFi IP (e.g. 192.168.1.105 on Pardeep)
 *   2. Derive subnet prefix (e.g. 192.168.1)
 *   3. Probe .1 to .254 with priority on common PC IPs
 *   4. When found → inject URL into WebView → load app
 *   5. If not found → show "Connecting..." and retry in 10s
 */
class MainActivity : ComponentActivity() {

    companion object {
        const val GATEWAY_PORT  = 9900
        const val PROBE_TIMEOUT = 500      // ms per host — generous for LAN
        const val PROBE_PATH    = "/actuator/info"
        const val RETRY_DELAY   = 10_000L  // ms between full scans
    }

    @Volatile private var resolvedBase: String = ""
    @Volatile private var webViewRef: WebView? = null

    /** Bridge exposed as window.AndroidBridge in JavaScript */
    inner class EntityKartBridge {
        @JavascriptInterface
        fun getApiBase(): String = resolvedBase

        @JavascriptInterface
        fun getAppVersion(): String = "3.0.0"

        @JavascriptInterface
        fun isLocalNetwork(): Boolean = resolvedBase.startsWith("http://")
    }

    // ─────────────────────────────────────────────────────────────────────────
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val activity = this

        setContent {
            // State: true = still scanning, false = gateway found, load app
            var isScanning by remember { mutableStateOf(true) }
            var statusMsg  by remember { mutableStateOf("Connecting to local server…") }

            // Start background scan
            LaunchedEffect(Unit) {
                launch(Dispatchers.IO) {
                    var attempt = 1
                    while (true) {
                        withContext(Dispatchers.Main) {
                            statusMsg = if (attempt == 1)
                                "Scanning local network…"
                            else
                                "Scanning… (attempt $attempt)"
                        }
                        val found = activity.discoverBackend()
                        if (found.isNotEmpty()) {
                            activity.resolvedBase = found
                            withContext(Dispatchers.Main) {
                                isScanning = false
                                // Also inject into already-loaded webview if any
                                webViewRef?.let { activity.injectApiBase(it) }
                            }
                            break
                        } else {
                            withContext(Dispatchers.Main) {
                                statusMsg = "Gateway not found. Retrying in 10s…\n(Make sure Docker is running on your PC)"
                            }
                            kotlinx.coroutines.delay(RETRY_DELAY)
                            attempt++
                        }
                    }
                }
            }

            if (isScanning) {
                // ── Loading / Scan Screen ─────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1a1a2e)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Text(
                            text = "EntityKart",
                            color = Color(0xFFFF6B35),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        CircularProgressIndicator(
                            color = Color(0xFFFF6B35),
                            modifier = Modifier.size(56.dp),
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = statusMsg,
                            color = Color(0xFFcccccc),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Text(
                            text = "Ensure your phone and PC are on\nthe same WiFi network",
                            color = Color(0xFF666666),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 40.dp)
                        )
                    }
                }
            } else {
                // ── WebView: load app once gateway is known ───────────────────
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val webView = WebView(ctx)
                        activity.webViewRef = webView

                        webView.settings.apply {
                            javaScriptEnabled         = true
                            domStorageEnabled          = true
                            allowFileAccess            = false
                            allowContentAccess         = false
                            mixedContentMode           = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            setSupportZoom(false)
                            builtInZoomControls        = false
                            displayZoomControls        = false
                            cacheMode                  = WebSettings.LOAD_DEFAULT
                            useWideViewPort            = true
                            loadWithOverviewMode       = true
                            userAgentString            = "${userAgentString} EntityKartApp/3.0.0"
                        }

                        webView.addJavascriptInterface(activity.EntityKartBridge(), "AndroidBridge")

                        webView.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean = false

                            override fun onPageFinished(view: WebView, url: String) {
                                super.onPageFinished(view, url)
                                activity.injectApiBase(view)
                            }
                        }

                        // Inject before first load so app.js constant is already set
                        activity.injectApiBase(webView)
                        webView.loadUrl("file:///android_asset/index.html")
                        webView
                    }
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inject the resolved base URL into the running WebView page via JS
    // ─────────────────────────────────────────────────────────────────────────
    fun injectApiBase(view: WebView) {
        val base = resolvedBase
        if (base.isEmpty()) return

        val parts = base.removePrefix("http://").split(":")
        val ip    = parts.getOrNull(0) ?: ""
        val port  = parts.getOrNull(1) ?: "$GATEWAY_PORT"

        val js = """
            (function() {
                window.ENTITYKART_API_BASE = '$base';
                try {
                    localStorage.setItem('API_IP',   '$ip');
                    localStorage.setItem('API_PORT', '$port');
                    localStorage.removeItem('RENDER_DEPLOY');
                } catch(e) {}
                console.log('[EntityKart] API_BASE injected: $base');
            })();
        """.trimIndent()
        view.evaluateJavascript(js, null)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scan phone's current WiFi subnet for the EntityKart gateway
    // Returns "http://IP:PORT" if found, or "" if not found
    // ─────────────────────────────────────────────────────────────────────────
    private suspend fun discoverBackend(): String = withContext(Dispatchers.IO) {
        val subnets = getLocalSubnets()
        if (subnets.isEmpty()) return@withContext ""

        // Probe common PC IPs first (most home routers assign these)
        // PC is typically .1, .2, .10, .20, .23, .100, .101, .200 etc.
        val priorityLast = listOf(
            1, 2, 3, 4, 5, 10, 11, 20, 21, 22, 23, 24, 25, 30,
            50, 100, 101, 102, 103, 104, 105, 150, 200, 210, 220, 240, 250, 254
        )
        // Remaining IPs scanned after priority list
        val remaining = (6..254).filterNot { it in priorityLast }
        val fullList  = priorityLast + remaining

        for (subnet in subnets) {
            for (host in fullList) {
                val ip = "$subnet.$host"
                if (probeGateway(ip)) {
                    return@withContext "http://$ip:$GATEWAY_PORT"
                }
            }
        }
        return@withContext ""
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Returns /24 subnet prefixes for all active WiFi/mobile network interfaces
    // Excludes loopback, Docker (172.x), link-local (169.254.x)
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
                    // Exclude Docker/WSL (172.x) and link-local (169.254.x)
                    if (prefix.startsWith("172.") || prefix.startsWith("169.254")) continue
                    result.add(prefix)
                }
            }
        } catch (_: Exception) {}
        return result.distinct()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Probe whether the EntityKart gateway responds on http://{ip}:9900
    // HTTP 200–403 = gateway is up; 401 from JWT filter = gateway is running
    // ─────────────────────────────────────────────────────────────────────────
    private fun probeGateway(ip: String): Boolean {
        return try {
            val url  = URL("http://$ip:$GATEWAY_PORT$PROBE_PATH")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = PROBE_TIMEOUT
            conn.readTimeout    = PROBE_TIMEOUT
            conn.requestMethod  = "GET"
            conn.instanceFollowRedirects = false
            conn.connect()
            val code = conn.responseCode
            conn.disconnect()
            code in 200..403
        } catch (_: Exception) {
            false
        }
    }
}
