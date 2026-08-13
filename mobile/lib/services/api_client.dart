import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import 'auth_service.dart';

/// Backend'e giden tüm isteklerin ortak noktası: base URL, Authorization header
/// enjeksiyonu ve 401/403'te oturum-sona-erdi bildirimi burada tek yerde yönetilir.
/// Ağ hataları (SocketException/http.ClientException) yakalanmaz — çağıranların
/// offline-cache fallback mantığı bu exception'ları görmeye devam eder.
class ApiClient {
  static const String baseUrl = 'https://fisbu-production-613c.up.railway.app';

  /// 401/403 alındığında (yalnızca Authorization header'ı eklenmiş bir istekte)
  /// tetiklenir — AuthWrapper bunu dinleyip kullanıcıyı login'e yönlendirir.
  /// ThemeController/ConnectivityService ile aynı ValueNotifier deseni.
  static final ValueNotifier<int> sessionExpired = ValueNotifier<int>(0);

  static Future<Map<String, String>> _headers({required bool auth, bool json = false}) async {
    final headers = <String, String>{
      if (json) 'Content-Type': 'application/json',
    };
    if (auth) {
      final token = await AuthService.getToken();
      if (token != null) headers['Authorization'] = 'Bearer $token';
    }
    return headers;
  }

  static Uri _uri(String path, [Map<String, String>? query]) {
    final uri = Uri.parse('$baseUrl$path');
    return (query == null || query.isEmpty) ? uri : uri.replace(queryParameters: query);
  }

  static void _checkSession(http.Response response, Map<String, String> headers) {
    final hadAuth = headers.containsKey('Authorization');
    if (hadAuth && (response.statusCode == 401 || response.statusCode == 403)) {
      sessionExpired.value++;
    }
  }

  static Future<http.Response> get(String path, {Map<String, String>? query, bool auth = true}) async {
    final headers = await _headers(auth: auth);
    final response = await http.get(_uri(path, query), headers: headers);
    _checkSession(response, headers);
    return response;
  }

  static Future<http.Response> post(String path, {Object? body, bool auth = true}) async {
    final headers = await _headers(auth: auth, json: true);
    final response = await http.post(_uri(path), headers: headers, body: body != null ? jsonEncode(body) : null);
    _checkSession(response, headers);
    return response;
  }

  static Future<http.Response> put(String path, {Object? body, bool auth = true}) async {
    final headers = await _headers(auth: auth, json: true);
    final response = await http.put(_uri(path), headers: headers, body: body != null ? jsonEncode(body) : null);
    _checkSession(response, headers);
    return response;
  }

  static Future<http.Response> delete(String path, {bool auth = true}) async {
    final headers = await _headers(auth: auth, json: true);
    final response = await http.delete(_uri(path), headers: headers);
    _checkSession(response, headers);
    return response;
  }

  /// Tek dosyalık multipart POST (fotoğraf/ekstre yükleme).
  static Future<http.Response> postMultipart(String path, {required http.MultipartFile file}) async {
    final request = http.MultipartRequest('POST', _uri(path));
    final headers = await _headers(auth: true);
    request.headers.addAll(headers);
    request.files.add(file);
    final streamed = await request.send();
    final response = await http.Response.fromStream(streamed);
    _checkSession(response, headers);
    return response;
  }

  /// Başarısız bir response'un `{"error": "..."}` gövdesinden mesajı çıkarır.
  /// Gövde boş ya da JSON değilse (ör. token mid-session geçersiz kılındığında
  /// Spring Security'nin döndürdüğü boş 401/403 body'si) [fallback] döner.
  static String errorMessage(http.Response response, {String fallback = 'Bilinmeyen hata'}) {
    if (response.body.isEmpty) return fallback;
    try {
      final body = jsonDecode(response.body) as Map<String, dynamic>;
      return body['error'] as String? ?? fallback;
    } catch (_) {
      return fallback;
    }
  }
}
