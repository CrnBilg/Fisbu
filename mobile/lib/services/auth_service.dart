import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class AuthService {
  static const String _baseUrl = 'https://fisbu-production-613c.up.railway.app';
  static const String _tokenKey = 'jwt_token';
  static const _storage = FlutterSecureStorage();

  // Bellekte önbelleklenir, ilk okumada secure storage'dan yüklenir
  static String? _token;

  /// Başarısız bir response'un body'sinden hata mesajını çıkarır. Body boş ya
  /// da JSON olarak ayrıştırılamıyorsa (ör. token mid-session geçersiz kılındığında
  /// Spring Security'nin döndürdüğü boş 401/403 body'si) ham FormatException
  /// kullanıcıya sızmaz, [fallback] döner.
  static String _parseErrorMessage(http.Response response, {String fallback = 'Bilinmeyen hata'}) {
    if (response.body.isEmpty) return fallback;
    try {
      final body = jsonDecode(response.body) as Map<String, dynamic>;
      return body['error'] as String? ?? fallback;
    } catch (_) {
      return fallback;
    }
  }

  static Future<AuthResult> login(String email, String password) async {
    try {
      final response = await http.post(
        Uri.parse('$_baseUrl/auth/login'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'email': email, 'password': password}),
      );
      if (response.statusCode == 200) {
        final body = jsonDecode(response.body) as Map<String, dynamic>;
        _token = body['token'] as String;
        await _storage.write(key: _tokenKey, value: _token);
        return AuthResult(success: true);
      } else {
        return AuthResult(success: false, errorMessage: _parseErrorMessage(response));
      }
    } catch (e) {
      return AuthResult(success: false, errorMessage: 'Bağlantı hatası: $e');
    }
  }

  static Future<AuthResult> register(String email, String password, {String? name}) async {
    try {
      final response = await http.post(
        Uri.parse('$_baseUrl/auth/register'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'email': email, 'password': password, if (name != null) 'name': name}),
      );
      if (response.statusCode == 200) {
        return AuthResult(success: true);
      } else {
        return AuthResult(success: false, errorMessage: _parseErrorMessage(response));
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
      final response = await http.get(
        Uri.parse('$_baseUrl/auth/profile'),
        headers: {'Authorization': 'Bearer $token'},
      );
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
      final response = await http.post(
        Uri.parse('$_baseUrl/auth/change-password'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: jsonEncode({
          'currentPassword': currentPassword,
          'newPassword': newPassword,
        }),
      );
      if (response.statusCode == 200) {
        return AuthResult(success: true);
      } else {
        final error = _parseErrorMessage(
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
      final response = await http.get(
        Uri.parse('$_baseUrl/auth/profile'),
        headers: {'Authorization': 'Bearer $token'},
      );
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
    final response = await http.get(
      Uri.parse('$_baseUrl/users/me/export'),
      headers: {'Authorization': 'Bearer $token'},
    );
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
      final response = await http.put(
        Uri.parse('$_baseUrl/auth/profile'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
        body: jsonEncode({
          if (name != null) 'name': name,
          if (profileImageUrl != null) 'profileImageUrl': profileImageUrl,
        }),
      );
      return response.statusCode == 200;
    } catch (e) {
      return false;
    }
  }

  static Future<AuthResult> forgotPassword(String email) async {
    try {
      final response = await http.post(
        Uri.parse('$_baseUrl/auth/forgot-password'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'email': email}),
      );
      if (response.statusCode == 200) {
        return AuthResult(success: true);
      } else {
        return AuthResult(success: false, errorMessage: _parseErrorMessage(response));
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
      final response = await http.post(
        Uri.parse('$_baseUrl/auth/reset-password'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'email': email,
          'code': code,
          'newPassword': newPassword,
        }),
      );
      if (response.statusCode == 200) {
        return AuthResult(success: true);
      } else {
        return AuthResult(success: false, errorMessage: _parseErrorMessage(response));
      }
    } catch (e) {
      return AuthResult(success: false, errorMessage: 'Bağlantı hatası: $e');
    }
  }

  static Future<AuthResult> resendVerificationCode(String email) async {
    try {
      final response = await http.post(
        Uri.parse('$_baseUrl/auth/resend-verification'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'email': email}),
      );
      if (response.statusCode == 200) {
        return AuthResult(success: true);
      } else {
        return AuthResult(success: false, errorMessage: _parseErrorMessage(response));
      }
    } catch (e) {
      return AuthResult(success: false, errorMessage: 'Bağlantı hatası: $e');
    }
  }

  static Future<AuthResult> verifyEmail(String email, String code) async {
    try {
      final response = await http.post(
        Uri.parse('$_baseUrl/auth/verify-email'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'email': email, 'code': code}),
      );
      if (response.statusCode == 200) {
        return AuthResult(success: true);
      } else {
        return AuthResult(success: false, errorMessage: _parseErrorMessage(response));
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
      final response = await http.delete(
        Uri.parse('$_baseUrl/auth/account'),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $token',
        },
      );
      if (response.statusCode == 200 || response.statusCode == 204) {
        await logout();
        return AuthResult(success: true);
      } else {
        final error = _parseErrorMessage(
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
