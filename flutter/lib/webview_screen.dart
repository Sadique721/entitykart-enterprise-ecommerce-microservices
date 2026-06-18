import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:webview_flutter/webview_flutter.dart';

// ============================================================
// EntityKart WebView Screen
// v1.5.0 Bug Fixes:
// - Bridge injection now on onPageFinished (DOM ready) instead of onPageStarted
// - Android back button handled with WillPopScope / NavigatorObserver
// - IP loaded from SharedPreferences (configurable via Settings)
// - Pull-to-refresh added
// ============================================================

class WebViewScreen extends StatefulWidget {
  const WebViewScreen({Key? key}) : super(key: key);

  @override
  State<WebViewScreen> createState() => _WebViewScreenState();
}

class _WebViewScreenState extends State<WebViewScreen> {
  late final WebViewController _controller;
  bool _isLoading = true;
  bool _isRefreshing = false;
  String _backendApiBase = '';
  String _frontendUrl = 'file:///android_asset/index.html';

  @override
  void initState() {
    super.initState();
    _loadIpConfig().then((_) => _initWebView());
  }

  // ---- Load backend IP from SharedPreferences ----
  Future<void> _loadIpConfig() async {
    final prefs = await SharedPreferences.getInstance();
    final savedIp = prefs.getString('backend_ip') ?? '';
    final savedPort = prefs.getString('backend_port') ?? '9080';
    if (savedIp.isNotEmpty) {
      setState(() {
        _backendApiBase = 'http://$savedIp:$savedPort';
      });
    } else {
      // Default: use Docker API Gateway port
      setState(() {
        _backendApiBase = 'http://10.0.2.2:9080';
      });
    }
  }

  void _initWebView() {
    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(const Color(0xFF0a0a0f))
      ..setNavigationDelegate(
        NavigationDelegate(
          onPageStarted: (url) {
            if (mounted) setState(() => _isLoading = true);
          },
          onPageFinished: (url) {
            // BUG FIX: Inject bridge AFTER page is fully loaded (DOM ready)
            if (mounted) setState(() => _isLoading = false);
            _injectAndroidBridge();
          },
          onWebResourceError: (error) {
            debugPrint('[EntityKart WebView] Error: ${error.description}');
            if (mounted) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text('Page error: ${error.description}'),
                  backgroundColor: Colors.red[700],
                ),
              );
            }
          },
        ),
      )
      ..addJavaScriptChannel(
        'AndroidBridge',
        onMessageReceived: (message) {
          debugPrint('[AndroidBridge] Received: ${message.message}');
          // Handle any messages sent from the JS layer
        },
      )
      ..loadRequest(Uri.parse(_frontendUrl));
  }

  // ---- BUG FIX: Inject bridge after DOM is ready (onPageFinished) ----
  Future<void> _injectAndroidBridge() async {
    final js = '''
      (function() {
        if (window.AndroidBridgeReady) return;
        window.AndroidBridgeReady = true;

        // Expose backend config to AngularJS app
        window.AndroidConfig = {
          apiBase: "${_backendApiBase.isNotEmpty ? _backendApiBase : 'http://10.0.2.2:9080'}",
          platform: "flutter",
          version: "1.5.0"
        };

        // Override localStorage key used by app.js for API Base URL
        if ("${_backendApiBase}".length > 0) {
          try {
            localStorage.setItem('ekApiPort', '${_backendApiBase.contains("9080") ? "9080" : "9901"}');
            console.log('[Bridge] Backend API set to: ' + window.AndroidConfig.apiBase);
          } catch(e) {}
        }
      })();
    ''';

    try {
      await _controller.runJavaScript(js);
      debugPrint('[EntityKart] Android bridge injected. Backend: $_backendApiBase');
    } catch (e) {
      debugPrint('[EntityKart] Bridge injection error: $e');
    }
  }

  // ---- Pull to Refresh ----
  Future<void> _handleRefresh() async {
    setState(() => _isRefreshing = true);
    await _controller.reload();
    if (mounted) setState(() => _isRefreshing = false);
  }

  // ---- Handle Android Back Button ----
  Future<bool> _handleBackPressed() async {
    if (await _controller.canGoBack()) {
      await _controller.goBack();
      return false; // Do not pop the screen
    }
    return true; // Allow pop (exit app)
  }

  @override
  Widget build(BuildContext context) {
    return WillPopScope(
      onWillPop: _handleBackPressed,
      child: Scaffold(
        backgroundColor: const Color(0xFF0a0a0f),
        body: SafeArea(
          child: RefreshIndicator(
            onRefresh: _handleRefresh,
            color: const Color(0xFFFF6B35),
            backgroundColor: const Color(0xFF1a1a2e),
            child: Stack(
              children: [
                // WebView
                WebViewWidget(controller: _controller),

                // Loading overlay
                if (_isLoading)
                  Container(
                    color: const Color(0xFF0a0a0f),
                    child: Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Container(
                            width: 80,
                            height: 80,
                            decoration: BoxDecoration(
                              borderRadius: BorderRadius.circular(20),
                              gradient: const LinearGradient(
                                colors: [Color(0xFFFF6B35), Color(0xFF8B5CF6)],
                                begin: Alignment.topLeft,
                                end: Alignment.bottomRight,
                              ),
                              boxShadow: [
                                BoxShadow(
                                  color: const Color(0xFFFF6B35).withOpacity(0.4),
                                  blurRadius: 20,
                                  spreadRadius: 2,
                                ),
                              ],
                            ),
                            child: const Icon(
                              Icons.shopping_bag_rounded,
                              color: Colors.white,
                              size: 40,
                            ),
                          ),
                          const SizedBox(height: 24),
                          const Text(
                            'EntityKart',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 26,
                              fontWeight: FontWeight.w800,
                              letterSpacing: 1,
                            ),
                          ),
                          const SizedBox(height: 8),
                          const Text(
                            'Loading your store...',
                            style: TextStyle(
                              color: Color(0xFF6B7280),
                              fontSize: 14,
                            ),
                          ),
                          const SizedBox(height: 32),
                          const SizedBox(
                            width: 40,
                            height: 40,
                            child: CircularProgressIndicator(
                              valueColor: AlwaysStoppedAnimation<Color>(Color(0xFFFF6B35)),
                              strokeWidth: 3,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
