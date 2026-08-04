import 'dart:convert';
import 'dart:math';
import 'package:crypto/crypto.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Face ID/Touch ID başarısız olduğunda kullanılabilecek yedek PIN kilidi.
/// PIN asla düz metin olarak saklanmaz — rastgele salt + SHA-256 hash tutulur,
/// hem hash hem salt cihazın güvenli deposunda (Keychain/Keystore) saklanır.
class PinService {
  static const _storage = FlutterSecureStorage();
  static const _hashKey = 'pin_hash';
  static const _saltKey = 'pin_salt';

  static Future<bool> isPinSet() async {
    final hash = await _storage.read(key: _hashKey);
    return hash != null;
  }

  static Future<void> setPin(String pin) async {
    final salt = _generateSalt();
    final hash = _hashPin(pin, salt);
    await _storage.write(key: _saltKey, value: salt);
    await _storage.write(key: _hashKey, value: hash);
  }

  static Future<void> clearPin() async {
    await _storage.delete(key: _hashKey);
    await _storage.delete(key: _saltKey);
  }

  static Future<bool> verifyPin(String pin) async {
    final salt = await _storage.read(key: _saltKey);
    final storedHash = await _storage.read(key: _hashKey);
    if (salt == null || storedHash == null) return false;
    return _hashPin(pin, salt) == storedHash;
  }

  static String _generateSalt() {
    final random = Random.secure();
    final bytes = List<int>.generate(16, (_) => random.nextInt(256));
    return base64Url.encode(bytes);
  }

  static String _hashPin(String pin, String salt) {
    final bytes = utf8.encode('$salt:$pin');
    return sha256.convert(bytes).toString();
  }
}
