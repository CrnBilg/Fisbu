import 'dart:convert';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'api_client.dart';

class AuthService {
  static const String _tokenKey = 'jwt_token';
  static const _storage = FlutterSecureStorage();

  // Bellekte önbelleklenir, ilk okumada secure storage'dan yüklenir
  static String? _token;

  static Future<AuthResult> login(String email, String password) async {
    try {
      final response = await ApiClient.post('/auth/login',
          auth: false, body: {'email': email, 'password': password});
      if (response.statusCode == 200) {
        final body = jsonDecode(response.body) as Map<String, dynamic>;
        _token = body['token'] as String;
        await _storage.write(key: _tokenKey, value: _token);
        return AuthResult(success: true);
      } else {
        return AuthResult(success: false, errorMessage: ApiClient.errorMessage(response));
      }
    } catch (e) {
      return AuthResult(success: false, errorMessage: 'Bağlantı hatası: $e');
    }
  }

  static Future<AuthResult> register(String email, String password, {String? name}) async {
    try {
      final response = await ApiClient.post('/auth/register',
          auth: false, body: {'email': email, 'password': password, if (name != null) 'name': name});
      if (response.statusCode == 200) {
        return AuthResult(success: true);
      } else {
        return AuthResult(success: false, errorMessage: ApiClient.errorMessage(response));
      }
    } catch (e) {
      return AuthResult(success: false, errorMessage: 'Bağlantı hatası: $e');
    }
  }

  static Future<String?> getToken() async {
    if (_token != null) return _token;
    try {
      _token = await _storage.read(key: _tokenKey);
    } catch (e) {
      _token = null;
    }
    return _token;
  }

  static Future<void> logout() async {
    _token = null;
    try {
      await _storage.delete(key: _tokenKey);
    } catch (e) {}
  }

  static Future<bool> isLoggedIn() async => await getToken() != null;

  /// Cihazda saklanan token'ın hâlâ sunucu tarafında geçerli olup olmadığını kontrol eder.
  /// true: geçerli, false: süresi dolmuş/geçersiz (401/403 — çıkış yapılmalı),
  /// null: sunucuya ulaşılamadı (ağ hatası — token hakkında karar verilemez, olduğu gibi bırak)
  static Future<bool?> validateSession() async {
    final token = await getToken();
    if (token == null) return false;
    try {
      final response = await ApiClient.get('/auth/profile');
      if (response.statusCode == 200) return true;
      if (response.statusCode == 401 || response.statusCode == 403) return false;
      return null;
    } catch (e) {
      return null;
    }
  }

  static Future<AuthResult> changePassword(
    String currentPassword,
    String newPassword,
  ) async {
    final token = await getToken();
    if (token == null) {
      return AuthResult(success: false, errorMessage: 'Giriş yapılmamış');
    }
    try {
      final response = await ApiClient.post('/auth/change-password', body: {
        'currentPassword': currentPassword,
        'newPassword': newPassword,
      });
      if (response.statusCode == 200) {
        return AuthResult(success: true);
      } else {
        final error = ApiClient.errorMessage(
          response,
          fallback: 'Oturum süresi dolmuş olabilir, lütfen tekrar giriş yap',
        );
        return AuthResult(success: false, errorMessage: error);
      }
    } catch (e) {
      return AuthResult(success: false, errorMessage: 'Bağlantı hatası: $e');
    }
  }

  static Future<Map<String, dynamic>?> getProfile() async {
    final token = await getToken();
    if (token == null) return null;
    try {
      final response = await ApiClient.get('/auth/profile');
      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      }
    } catch (e) {}
    return null;
  }

  /// KVKK "verilerimi indir" — profil/kategori/bütçe/fiş verilerinin tamamını
  /// biçimlendirilmiş JSON metni olarak döner (dosyaya yazıp paylaşmak için).
  static Future<String> downloadMyData() async {
    final token = await getToken();
    if (token == null) {
      throw Exception('Oturum süresi doldu, lütfen tekrar giriş yapın');
    }
    final response = await ApiClient.get('/users/me/export');
    if (response.statusCode == 200) {
      final decoded = jsonDecode(response.body);
      return const JsonEncoder.withIndent('  ').convert(decoded);
    } else if (response.statusCode == 401 || response.statusCode == 403) {
      throw Exception('Oturum süresi doldu, lütfen tekrar giriş yapın');
    } else {
      throw Exception('Verileriniz indirilemedi: ${response.statusCode}');
    }
  }

  static Future<bool> updateProfile({String? name, String? profileImageUrl}) async {
    final token = await getToken();
    if (token == null) return false;
    try {
      final response = await ApiClient.put('/auth/profile', body: {
        if (name != null) 'name': name,
        if (profileImageUrl != null) 'profileImageUrl': profileImageUrl,
      });
      return response.statusCode == 200;
    } catch (e) {
      return false;
    }
  }

  static Future<AuthResult> forgotPassword(String email) async {
    try {
      final response = await ApiClient.post('/auth/forgot-password', auth: false, body: {'email': email});
      if (response.statusCode == 200) {
        return AuthResult(success: true);
      } else {
        return AuthResult(success: false, errorMessage: ApiClient.errorMessage(response));
      }
    } catch (e) {
      return AuthResult(success: false, errorMessage: 'Bağlantı hatası: $e');
    }
  }

  static Future<AuthResult> resetPassword(
    String email,
    String code,
    String newPassword,
  ) async {
    try {
      final response = await ApiClient.post('/auth/reset-password', auth: false, body: {
        'email': email,
        'code': code,
        'newPassword': newPassword,
      });
      if (response.statusCode == 200) {
        return AuthResult(success: true);
      } else {
        return AuthResult(success: false, errorMessage: ApiClient.errorMessage(response));
      }
    } catch (e) {
      return AuthResult(success: false, errorMessage: 'Bağlantı hatası: $e');
    }
  }

  static Future<AuthResult> resendVerificationCode(String email) async {
    try {
      final response = await ApiClient.post('/auth/resend-verification', auth: false, body: {'email': email});
      if (response.statusCode == 200) {
        return AuthResult(success: true);
      } else {
        return AuthResult(success: false, errorMessage: ApiClient.errorMessage(response));
      }
    } catch (e) {
      return AuthResult(success: false, errorMessage: 'Bağlantı hatası: $e');
    }
  }

  static Future<AuthResult> verifyEmail(String email, String code) async {
    try {
      final response =
          await ApiClient.post('/auth/verify-email', auth: false, body: {'email': email, 'code': code});
      if (response.statusCode == 200) {
        return AuthResult(success: true);
      } else {
        return AuthResult(success: false, errorMessage: ApiClient.errorMessage(response));
      }
    } catch (e) {
      return AuthResult(success: false, errorMessage: 'Bağlantı hatası: $e');
    }
  }

  static Future<AuthResult> deleteAccount() async {
    final token = await getToken();
    if (token == null) {
      return AuthResult(success: false, errorMessage: 'Giriş yapılmamış');
    }
    try {
      final response = await ApiClient.delete('/auth/account');
      if (response.statusCode == 200 || response.statusCode == 204) {
        await logout();
        return AuthResult(success: true);
      } else {
        final error = ApiClient.errorMessage(
          response,
          fallback: 'Oturum süresi dolmuş olabilir, lütfen tekrar giriş yap',
        );
        return AuthResult(success: false, errorMessage: error);
      }
    } catch (e) {
      return AuthResult(success: false, errorMessage: 'Bağlantı hatası: $e');
    }
  }

  static Future<String?> getEmail() async {
    final token = await getToken();
    if (token == null) return null;
    try {
      final parts = token.split('.');
      if (parts.length != 3) return null;
      final payload = parts[1];
      final normalized = base64Url.normalize(payload);
      final decoded = utf8.decode(base64Url.decode(normalized));
      final data = jsonDecode(decoded) as Map<String, dynamic>;
      return data['sub'] as String?;
    } catch (e) {
      return null;
    }
  }
}

class AuthResult {
  final bool success;
  final String? errorMessage;
  AuthResult({required this.success, this.errorMessage});
}
