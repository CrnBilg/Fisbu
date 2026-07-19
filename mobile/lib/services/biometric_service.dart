import 'package:local_auth/local_auth.dart';
import 'package:shared_preferences/shared_preferences.dart';

class BiometricService {
  static const String _enabledKey = 'biometric_enabled';
  static final LocalAuthentication _auth = LocalAuthentication();

  /// Cihaz biyometrik girişi destekliyor ve en az bir biyometri kayıtlı mı?
  static Future<bool> isDeviceSupported() async {
    try {
      final canCheck = await _auth.canCheckBiometrics;
      final isSupported = await _auth.isDeviceSupported();
      return canCheck && isSupported;
    } catch (e) {
      return false;
    }
  }

  /// Kullanıcının profil ekranından açtığı tercih.
  static Future<bool> isEnabled() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getBool(_enabledKey) ?? false;
  }

  static Future<void> setEnabled(bool enabled) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(_enabledKey, enabled);
  }

  /// Biyometrik doğrulama iste. Cihaz desteklemiyorsa ya da kayıtlı
  /// biyometri yoksa kullanıcıyı kilitlemeden true döner.
  static Future<bool> authenticate() async {
    if (!await isDeviceSupported()) return true;
    try {
      return await _auth.authenticate(
        localizedReason: 'Hesabına giriş yapmak için kimliğini doğrula',
        options: const AuthenticationOptions(
          biometricOnly: false,
          stickyAuth: true,
        ),
      );
    } catch (e) {
      return false;
    }
  }
}
