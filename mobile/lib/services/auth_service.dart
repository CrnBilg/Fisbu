import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class AuthService {
  static const String _baseUrl = 'https://fisbu-production-613c.up.railway.app';
  static const _storage = FlutterSecureStorage();
  static const String _tokenKey = 'jwt_token';

  static Future<AuthResult> login(String email, String password) async {
    try {
      final response = await http.post(
        Uri.parse('$_baseUrl/auth/login'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'email': email, 'password': password}),
      );

      final body = jsonDecode(response.body) as Map<String, dynamic>;

      if (response.statusCode == 200) {
        final token = body['token'] as String;
        await _storage.write(key: _tokenKey, value: token);
        return AuthResult(success: true);
      } else {
        final error = body['error'] as String? ?? 'Bilinmeyen hata';
        return AuthResult(success: false, errorMessage: error);
      }
    } catch (e) {
      return AuthResult(success: false, errorMessage: 'Bağlantı hatası: $e');
    }
  }


  static Future<AuthResult> register(String email, String password) async {
    try {
      final response = await http.post(
        Uri.parse('$_baseUrl/auth/register'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'email': email, 'password': password}),
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
    return await _storage.read(key: _tokenKey);
  }

  static Future<void> logout() async {
    await _storage.delete(key: _tokenKey);
  }

static Future<bool> isLoggedIn() async {
    final token = await getToken();
  
    return token != null;
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