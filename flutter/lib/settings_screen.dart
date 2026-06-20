import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

// ============================================================
// Settings Screen — Configure Backend IP/Port
// Allows the user to enter a custom backend API IP address
// so the Flutter app can connect to any machine on the LAN.
// ============================================================

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({Key? key}) : super(key: key);

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  final _ipController = TextEditingController();
  final _portController = TextEditingController(text: '9080');
  bool _saved = false;
  String _currentTarget = '';

  @override
  void initState() {
    super.initState();
    _loadSavedSettings();
  }

  Future<void> _loadSavedSettings() async {
    final prefs = await SharedPreferences.getInstance();
    final ip = prefs.getString('backend_ip') ?? '';
    final port = prefs.getString('backend_port') ?? '9080';
    setState(() {
      _ipController.text = ip;
      _portController.text = port;
      _currentTarget = ip.isNotEmpty ? 'http://$ip:$port' : 'https://entitykart-enterprise-ecommerce.onrender.com (Render Cloud)';
    });
  }

  Future<void> _saveSettings() async {
    final ip = _ipController.text.trim();
    final port = _portController.text.trim();

    if (ip.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter an IP address'), backgroundColor: Colors.red),
      );
      return;
    }

    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('backend_ip', ip);
    await prefs.setString('backend_port', port);

    setState(() {
      _saved = true;
      _currentTarget = 'http://$ip:$port';
    });

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Settings saved! Restart the app to connect to $ip:$port'),
        backgroundColor: const Color(0xFF10B981),
      ),
    );
  }

  Future<void> _resetToDefault() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('backend_ip');
    await prefs.remove('backend_port');
    setState(() {
      _ipController.clear();
      _portController.text = '9080';
      _currentTarget = 'https://entitykart-enterprise-ecommerce.onrender.com (Render Cloud)';
      _saved = false;
    });
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Reset to default Render Cloud connection')),
    );
  }

  @override
  Widget build(BuildContext context) {
    const accent = Color(0xFFFF6B35);
    const cardBg = Color(0xFF111118);
    const border = Color(0xFF1F1F2E);

    return Scaffold(
      backgroundColor: const Color(0xFF0a0a0f),
      appBar: AppBar(
        title: const Text('Settings'),
        backgroundColor: const Color(0xFF0a0a0f),
        leading: const Icon(Icons.settings, color: accent),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [

            // Header Card
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFFFF6B35), Color(0xFF8B5CF6)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(16),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(Icons.shopping_bag_rounded, color: Colors.white, size: 36),
                  const SizedBox(height: 10),
                  const Text(
                    'EntityKart',
                    style: TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.w800),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'v1.5.0 — Flutter WebView Client',
                    style: TextStyle(color: Colors.white.withOpacity(0.7), fontSize: 12),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 28),

            // Current Connection
            const Text(
              'Current API Connection',
              style: TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600, letterSpacing: 0.5),
            ),
            const SizedBox(height: 8),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: cardBg,
                border: Border.all(color: border),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Row(
                children: [
                  const Icon(Icons.link, color: accent, size: 18),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      _currentTarget,
                      style: const TextStyle(color: Colors.white, fontSize: 13, fontFamily: 'monospace'),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 28),

            // Backend IP Config
            const Text(
              'Backend API Server',
              style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 4),
            Text(
              'Enter your computer\'s LAN IP to connect from a physical device.',
              style: TextStyle(color: Colors.white.withOpacity(0.5), fontSize: 12),
            ),
            const SizedBox(height: 16),

            // IP Address Input
            _buildLabel('IP Address'),
            const SizedBox(height: 6),
            TextField(
              controller: _ipController,
              style: const TextStyle(color: Colors.white),
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: _inputDecoration('e.g. 192.168.1.6'),
            ),
            const SizedBox(height: 14),

            // Port Input
            _buildLabel('Port'),
            const SizedBox(height: 6),
            TextField(
              controller: _portController,
              style: const TextStyle(color: Colors.white),
              keyboardType: TextInputType.number,
              decoration: _inputDecoration('e.g. 9080 (Docker) or 9901 (Local)'),
            ),

            const SizedBox(height: 10),
            // Quick port presets
            Row(
              children: [
                _portChip('9080', 'Docker'),
                const SizedBox(width: 8),
                _portChip('9901', 'Local Dev'),
              ],
            ),

            const SizedBox(height: 28),

            // Save Button
            SizedBox(
              width: double.infinity,
              height: 52,
              child: ElevatedButton.icon(
                onPressed: _saveSettings,
                icon: const Icon(Icons.save_rounded),
                label: const Text('Save Settings', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700)),
                style: ElevatedButton.styleFrom(
                  backgroundColor: accent,
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                ),
              ),
            ),

            const SizedBox(height: 12),

            // Reset Button
            SizedBox(
              width: double.infinity,
              height: 44,
              child: TextButton.icon(
                onPressed: _resetToDefault,
                icon: const Icon(Icons.refresh_rounded, size: 18),
                label: const Text('Reset to Default (Emulator)'),
                style: TextButton.styleFrom(
                  foregroundColor: Colors.white54,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                    side: const BorderSide(color: Color(0xFF2A2A3E)),
                  ),
                ),
              ),
            ),

            const SizedBox(height: 36),

            // Help Section
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: const Color(0xFF0D1117),
                border: Border.all(color: const Color(0xFF21262D)),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Row(
                    children: [
                      Icon(Icons.help_outline, color: Colors.white54, size: 16),
                      SizedBox(width: 8),
                      Text('How to find your PC IP', style: TextStyle(color: Colors.white70, fontWeight: FontWeight.w600, fontSize: 13)),
                    ],
                  ),
                  const SizedBox(height: 10),
                  _helpStep('Windows', 'Open CMD → type ipconfig → look for IPv4 Address'),
                  _helpStep('Mac/Linux', 'Open Terminal → type ifconfig | grep inet'),
                  _helpStep('Then', 'Enter the IP above, select port 9080 (Docker) or 9901 (Local)'),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildLabel(String text) => Text(
        text,
        style: const TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w500),
      );

  InputDecoration _inputDecoration(String hint) => InputDecoration(
        hintText: hint,
        hintStyle: const TextStyle(color: Color(0xFF4B5563), fontSize: 13),
        filled: true,
        fillColor: const Color(0xFF111118),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFF1F1F2E)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFF1F1F2E)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFFFF6B35), width: 1.5),
        ),
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      );

  Widget _portChip(String port, String label) => GestureDetector(
        onTap: () => setState(() => _portController.text = port),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          decoration: BoxDecoration(
            color: _portController.text == port
                ? const Color(0xFFFF6B35).withOpacity(0.15)
                : const Color(0xFF111118),
            border: Border.all(
              color: _portController.text == port ? const Color(0xFFFF6B35) : const Color(0xFF1F1F2E),
            ),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Text(
            '$port ($label)',
            style: TextStyle(
              color: _portController.text == port ? const Color(0xFFFF6B35) : Colors.white54,
              fontSize: 12,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      );

  Widget _helpStep(String bold, String text) => Padding(
        padding: const EdgeInsets.only(bottom: 6),
        child: RichText(
          text: TextSpan(
            children: [
              TextSpan(
                text: '$bold: ',
                style: const TextStyle(color: Colors.white60, fontSize: 12, fontWeight: FontWeight.w600),
              ),
              TextSpan(
                text: text,
                style: const TextStyle(color: Colors.white38, fontSize: 12),
              ),
            ],
          ),
        ),
      );

  @override
  void dispose() {
    _ipController.dispose();
    _portController.dispose();
    super.dispose();
  }
}
