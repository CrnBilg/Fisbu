import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'auth_service.dart';

/// Bütçe push bildirim tercihlerini backend'de tutar (asıl kaynak burasıdır —
/// backend push göndermeden önce bu tercihlere bakar). SharedPreferences sadece
/// ekran açılırken ağ yokken gösterilecek son bilinen değeri önbelleklemek için.
class NotificationPrefsService {
  static const String _baseUrl = 'https://fisbu-production-613c.up.railway.app';
  static const String _budgetWarningKey = 'notif_budget_warning_enabled';
  static const String _budgetOverspendKey = 'notif_budget_overspend_enabled';

  static Future<bool> isBudgetWarningEnabled() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_budgetWarningKey) ?? true;
  }

  static Future<bool> isBudgetOverspendEnabled() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_budgetOverspendKey) ?? true;
  }

  /// Backend'den güncel tercihleri çeker ve yerel önbelleği günceller.
  /// Ağ hatasında son bilinen (önbellekteki) değerleri döner.
  static Future<({bool warning, bool overspend})> fetchPrefs() async {
    final prefs = await SharedPreferences.getInstance();
    try {
      final token = await AuthService.getToken();
      final response = await http.get(
        Uri.parse('$_baseUrl/users/notification-prefs'),
        headers: {'Authorization': 'Bearer $token'},
      );
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        final warning = data['budgetWarningEnabled'] as bool? ?? true;
        final overspend = data['budgetOverspendEnabled'] as bool? ?? true;
        await prefs.setBool(_budgetWarningKey, warning);
        await prefs.setBool(_budgetOverspendKey, overspend);
        return (warning: warning, overspend: overspend);
      }
    } catch (_) {
      // Ağ yoksa önbellekteki son bilinen değerlere düş
    }
    return (
      warning: prefs.getBool(_budgetWarningKey) ?? true,
      overspend: prefs.getBool(_budgetOverspendKey) ?? true,
    );
  }

  /// Tercihi hem backend'e hem yerel önbelleğe yazar.
  static Future<void> setBudgetWarningEnabled(bool enabled) async {
    final overspend = await isBudgetOverspendEnabled();
    await _syncToBackend(warning: enabled, overspend: overspend);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_budgetWarningKey, enabled);
  }

  static Future<void> setBudgetOverspendEnabled(bool enabled) async {
    final warning = await isBudgetWarningEnabled();
    await _syncToBackend(warning: warning, overspend: enabled);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_budgetOverspendKey, enabled);
  }

  static Future<void> _syncToBackend({required bool warning, required bool overspend}) async {
    final token = await AuthService.getToken();
    final response = await http.put(
      Uri.parse('$_baseUrl/users/notification-prefs'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
      body: jsonEncode({
        'budgetWarningEnabled': warning,
        'budgetOverspendEnabled': overspend,
      }),
    );
    if (response.statusCode != 200) {
      throw Exception('Bildirim tercihi kaydedilemedi: ${response.statusCode}');
    }
  }
}
