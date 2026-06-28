import 'dart:convert';
import 'package:http/http.dart' as http;

class AuthService {
  static const String _baseUrl = 'https://fisbu-production-613c.up.railway.app';
  static String? _token;

  static Future<AuthResult> login(String email, String password) async {
    try {
      final response = await http.post(
        Uri.parse('$_baseUrl/auth/login'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'email': email, 'password': password}),
      );
      final body = jsonDecode(response.body) as Map<String, dynamic>;
      if (response.statusCode == 200) {
        _token = body['token'] as String;
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

  static Future<String?> getToken() async => _token;

  static Future<void> logout() async => _token = null;

  static Future<bool> isLoggedIn() async => _token != null;

  static Future<String?> getEmail() async {
    if (_token == null) return null;
    try {
      final parts = _token!.split('.');
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