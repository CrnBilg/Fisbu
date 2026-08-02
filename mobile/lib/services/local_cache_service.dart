import 'package:hive_flutter/hive_flutter.dart';

/// Hive box'ları üzerinde ince bir wrapper. Codegen kullanmıyor, sadece
/// JSON-uyumlu (Map/List/String/num/bool) verileri saklıyor — projede zaten
/// build_runner/kod üretimi olmadığı için mevcut sadeliğe uyuyor.
class LocalCacheService {
  static const receiptsBox = 'receiptsBox';
  static const budgetsBox = 'budgetsBox';
  static const categoriesBox = 'categoriesBox';
  static const statsBox = 'statsBox';
  static const pendingReceiptsBox = 'pendingReceiptsBox';

  static Future<void> init() async {
    await Hive.initFlutter();
    await Hive.openBox(receiptsBox);
    await Hive.openBox(budgetsBox);
    await Hive.openBox(categoriesBox);
    await Hive.openBox(statsBox);
    await Hive.openBox(pendingReceiptsBox);
  }

  static Future<void> put(String boxName, String key, dynamic value) async {
    final box = Hive.box(boxName);
    await box.put(key, value);
    await box.put('${key}_syncedAt', DateTime.now().toIso8601String());
  }

  static dynamic get(String boxName, String key) {
    return Hive.box(boxName).get(key);
  }

  static DateTime? lastSyncedAt(String boxName, String key) {
    final raw = Hive.box(boxName).get('${key}_syncedAt') as String?;
    return raw != null ? DateTime.tryParse(raw) : null;
  }
}
