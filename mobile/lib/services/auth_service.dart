import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class AuthService {
  static const String _baseUrl = 'https://fisbu-production-613c.up.railway.app';
  static const String _tokenKey = 'jwt_token';
  static const _storage = FlutterSecureStorage();

  // Bellekte önbelleklenir, ilk okumada secure storage'dan yüklenir
  static String? _token;

  static Future<AuthResult> login(String email, String password) async {
    try {
      final response = await http.post(
        Uri.parse('$_baseUrl/auth/login'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'email': email, 'password': password, if (name != null) 'name': name}),
      );
      final body = jsonDecode(response.body) as Map<String, dynamic>;
      if (response.statusCode == 200) {
        _token = body['token'] as String;
        await _storage.write(key: _tokenKey, value: _token);
        return AuthResult(success: true);
      } else {
        final error = body['error'] as String? ?? 'Bilinmeyen hata';
        return AuthResult(success: false, errorMessage: error);
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
        final body = jsonDecode(response.body) as Map<String, dynamic>;
        final error = body['error'] as String? ?? 'Bilinmeyen hata';
        return AuthResult(success: false, errorMessage: error);
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

  static Future<AuthResult> changePassword(
      String currentPassword, String newPassword) async {
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
        final body = jsonDecode(response.body) as Map<String, dynamic>;
        final error = body['error'] as String? ?? 'Bilinmeyen hata';
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