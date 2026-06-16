import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';

class WebViewScreen extends StatefulWidget {
  const WebViewScreen({super.key});

  @override
  State<WebViewScreen> createState() => _WebViewScreenState();
}

class _WebViewScreenState extends State<WebViewScreen> {
  late final WebViewController _controller;
  bool _isLoading = true;

  // Change this to your PC's active IP and API Gateway port
  final String _backendApiBase = "http://192.168.1.6:9080";

  @override
  void initState() {
    super.initState();

    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(const Color(0xFF0A0D14)) // Match dark theme
      ..setNavigationDelegate(
        NavigationDelegate(
          onProgress: (int progress) {
            // Update loading state if needed
          },
          onPageStarted: (String url) {
            setState(() {
              _isLoading = true;
            });
            _injectBridge();
          },
          onPageFinished: (String url) {
            setState(() {
              _isLoading = false;
            });
            _injectBridge();
          },
          onWebResourceError: (WebResourceError error) {
            debugPrint("WebView Error: ${error.description}");
          },
          onNavigationRequest: (NavigationRequest request) {
            // Restrict navigation to local assets and the API gateway
            final url = request.url;
            final isAsset = url.startsWith('file:///');
            final isApi = url.startsWith(_backendApiBase);
            
            if (isAsset || isApi) {
              return NavigationDecision.navigate;
            }
            return NavigationDecision.prevent; // Block any unauthorized outgoing URLs
          },
        ),
      )
      ..loadFlutterAsset('assets/index.html');
  }

  void _injectBridge() {
    // Inject window.AndroidBridge mock object so AngularJS can consume getApiBase()
    final script = """
      window.ENTITYKART_API_BASE = '$_backendApiBase';
      window.AndroidBridge = {
        getApiBase: function() {
          return '$_backendApiBase';
        },
        getAppVersion: function() {
          return '1.0-flutter';
        }
      };
      console.log('Flutter AndroidBridge injected successfully');
    """;
    _controller.runJavaScript(script).catchError((e) {
      debugPrint("Error injecting Javascript Bridge: $e");
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0D14),
      body: SafeArea(
        child: Stack(
          children: [
            WebViewWidget(controller: _controller),
            if (_isLoading)
              const Center(
                child: CircularProgressIndicator(
                  valueColor: AlwaysStoppedAnimation<Color>(Color(0xFF6366F1)),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
