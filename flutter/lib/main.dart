import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'webview_screen.dart';
import 'settings_screen.dart';

// ============================================================
// EntityKart Flutter App Entry Point
// v1.5.0 — Fixed theme color (orange brand), added bottom nav bar, splash config
// ============================================================

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  // Lock to portrait mode for best mobile experience
  SystemChrome.setPreferredOrientations([
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
  ]);

  // Set status bar style to match dark theme
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.light,
      systemNavigationBarColor: Color(0xFF0a0a0f),
      systemNavigationBarIconBrightness: Brightness.light,
    ),
  );

  runApp(const EntityKartApp());
}

class EntityKartApp extends StatelessWidget {
  const EntityKartApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'EntityKart',
      debugShowCheckedModeBanner: false,
      // BUG FIX: Theme seed color corrected to brand orange (#FF6B35) instead of indigo
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFFFF6B35),
          brightness: Brightness.dark,
        ),
        useMaterial3: true,
        fontFamily: 'Roboto',
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFF0a0a0f),
          foregroundColor: Colors.white,
          elevation: 0,
          centerTitle: true,
          titleTextStyle: TextStyle(
            fontFamily: 'Roboto',
            fontSize: 18,
            fontWeight: FontWeight.w700,
            color: Colors.white,
          ),
        ),
        scaffoldBackgroundColor: const Color(0xFF0a0a0f),
        navigationBarTheme: NavigationBarThemeData(
          backgroundColor: const Color(0xFF111118),
          indicatorColor: const Color(0xFFFF6B35).withOpacity(0.15),
          labelTextStyle: MaterialStateProperty.all(
            const TextStyle(color: Colors.white70, fontSize: 11),
          ),
          iconTheme: MaterialStateProperty.all(
            const IconThemeData(color: Colors.white70),
          ),
        ),
      ),
      home: const MainShell(),
    );
  }
}

// ============================================================
// Main shell with bottom navigation bar
// ============================================================
class MainShell extends StatefulWidget {
  const MainShell({Key? key}) : super(key: key);

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  int _selectedIndex = 0;

  final List<Widget> _screens = const [
    WebViewScreen(),
    SettingsScreen(),
  ];

  final List<NavigationDestination> _destinations = const [
    NavigationDestination(
      icon: Icon(Icons.store_outlined),
      selectedIcon: Icon(Icons.store, color: Color(0xFFFF6B35)),
      label: 'Shop',
    ),
    NavigationDestination(
      icon: Icon(Icons.settings_outlined),
      selectedIcon: Icon(Icons.settings, color: Color(0xFFFF6B35)),
      label: 'Settings',
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _selectedIndex,
        children: _screens,
      ),
      bottomNavigationBar: Container(
        decoration: const BoxDecoration(
          border: Border(
            top: BorderSide(color: Color(0xFF1F1F2E), width: 1),
          ),
        ),
        child: NavigationBar(
          selectedIndex: _selectedIndex,
          onDestinationSelected: (index) {
            setState(() => _selectedIndex = index);
          },
          destinations: _destinations,
          backgroundColor: const Color(0xFF111118),
          height: 65,
          labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
        ),
      ),
    );
  }
}
