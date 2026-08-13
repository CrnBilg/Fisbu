import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:hive_flutter/hive_flutter.dart';

/// Hive box'ları üzerinde ince bir wrapper. Codegen kullanmıyor, sadece
/// JSON-uyumlu (Map/List/String/num/bool) verileri saklıyor — projede zaten
/// build_runner/kod üretimi olmadığı için mevcut sadeliğe uyuyor.
class LocalCacheService {
  static const receiptsBox = 'receiptsBox';
  static const budgetsBox = 'budgetsBox';
  static const categoriesBox = 'categoriesBox';
  static const pendingReceiptsBox = 'pendingReceiptsBox';

  static const _storage = FlutterSecureStorage();
  static const _encryptionKeyStorageKey = 'hive_encryption_key';

  static Future<void> init() async {
    await Hive.initFlutter();
    final cipher = HiveAesCipher(await _loadOrCreateEncryptionKey());
    await _openEncryptedBox(receiptsBox, cipher);
    await _openEncryptedBox(budgetsBox, cipher);
    await _openEncryptedBox(categoriesBox, cipher);
    await _openEncryptedBox(pendingReceiptsBox, cipher);
  }

  // Bu güncellemeden önce yüklenmiş uygulamalarda box'lar şifresiz yazılmıştı — şifreli
  // açmaya çalışmak o eski box'larda hataya yol açar. pendingReceiptsBox henüz backend'e
  // senkronize edilmemiş fişleri tutabileceğinden veri kaybetmeden şifreliye taşıyoruz.
  static Future<void> _openEncryptedBox(String name, HiveAesCipher cipher) async {
    try {
      await Hive.openBox(name, encryptionCipher: cipher);
      return;
    } catch (_) {
      // Şifresiz eski box — aşağıda migrate ediliyor
    }

    Map<dynamic, dynamic> legacyData = {};
    try {
      final legacyBox = await Hive.openBox(name);
      legacyData = Map.of(legacyBox.toMap());
      await legacyBox.close();
    } catch (_) {
      // Eski box de okunamıyorsa kurtarılacak veri yok, temiz şifreli box ile devam
    }

    await Hive.deleteBoxFromDisk(name);
    final box = await Hive.openBox(name, encryptionCipher: cipher);
    if (legacyData.isNotEmpty) {
      await box.putAll(legacyData);
    }
  }

  // Fiş/bütçe gibi finansal veriler cihazda düz metin durmasın diye Hive box'ları
  // AES ile şifreleniyor; anahtar Keychain/Keystore üzerinden flutter_secure_storage'da tutuluyor
  static Future<List<int>> _loadOrCreateEncryptionKey() async {
    final existing = await _storage.read(key: _encryptionKeyStorageKey);
    if (existing != null) {
      return base64Decode(existing);
    }
    final key = Hive.generateSecureKey();
    await _storage.write(key: _encryptionKeyStorageKey, value: base64Encode(key));
    return key;
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
