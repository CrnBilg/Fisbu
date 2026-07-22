import 'package:shared_preferences/shared_preferences.dart';

class NotificationPrefsService {
  static const String _budgetWarningKey = 'notif_budget_warning_enabled';
  static const String _budgetOverspendKey = 'notif_budget_overspend_enabled';

  /// Bütçe %80'e ulaştığında bildirim gönderilsin mi?
  static Future<bool> isBudgetWarningEnabled() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_budgetWarningKey) ?? true;
  }

  static Future<void> setBudgetWarningEnabled(bool enabled) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_budgetWarningKey, enabled);
  }

  /// Bütçe aşıldığında bildirim gönderilsin mi?
  static Future<bool> isBudgetOverspendEnabled() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_budgetOverspendKey) ?? true;
  }

  static Future<void> setBudgetOverspendEnabled(bool enabled) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_budgetOverspendKey, enabled);
  }
}
