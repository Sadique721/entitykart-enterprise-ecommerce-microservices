import 'package:flutter/material.dart';
import 'webview_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const EntitykartApp());
}

class EntitykartApp extends StatelessWidget {
  const EntitykartApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Entitykart',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF6366F1),
          brightness: Brightness.dark,
        ),
        useMaterial3: true,
      ),
      home: const WebViewScreen(),
    );
  }
}
