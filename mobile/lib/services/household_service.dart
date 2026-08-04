import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/household.dart';
import '../models/household_statistics.dart';
import 'auth_service.dart';

class HouseholdService {
  static const String _baseUrl = 'https://fisbu-production-613c.up.railway.app';

  static Future<Map<String, String>> _authHeaders({bool json = false}) async {
    final token = await AuthService.getToken();
    return {
      'Authorization': 'Bearer $token',
      if (json) 'Content-Type': 'application/json',
    };
  }

  static String _errorMessage(http.Response response, String fallback) {
    try {
      final body = jsonDecode(response.body);
      return body['error'] as String? ?? fallback;
    } catch (_) {
      return fallback;
    }
  }

  /// Kullanıcı bir aileye üye değilse null döner (404 hata değil, normal durum).
  static Future<Household?> getMyHousehold() async {
    final response = await http.get(
      Uri.parse('$_baseUrl/households/me'),
      headers: await _authHeaders(),
    );
    if (response.statusCode == 200) {
      return Household.fromJson(jsonDecode(response.body));
    } else if (response.statusCode == 404) {
      return null;
    } else {
      throw Exception(_errorMessage(response, 'Aile bilgisi alınamadı: ${response.statusCode}'));
    }
  }

  static Future<Household> createHousehold(String name) async {
    final response = await http.post(
      Uri.parse('$_baseUrl/households'),
      headers: await _authHeaders(json: true),
      body: jsonEncode({'name': name}),
    );
    if (response.statusCode == 201) {
      return Household.fromJson(jsonDecode(response.body));
    } else {
      throw Exception(_errorMessage(response, 'Aile oluşturulamadı: ${response.statusCode}'));
    }
  }

  static Future<Household> joinHousehold(String inviteCode) async {
    final response = await http.post(
      Uri.parse('$_baseUrl/households/join'),
      headers: await _authHeaders(json: true),
      body: jsonEncode({'inviteCode': inviteCode}),
    );
    if (response.statusCode == 200) {
      return Household.fromJson(jsonDecode(response.body));
    } else {
      throw Exception(_errorMessage(response, 'Aileye katılınamadı: ${response.statusCode}'));
    }
  }

  static Future<void> leaveHousehold() async {
    final response = await http.delete(
      Uri.parse('$_baseUrl/households/me'),
      headers: await _authHeaders(),
    );
    if (response.statusCode != 204) {
      throw Exception(_errorMessage(response, 'Aileden ayrılınamadı: ${response.statusCode}'));
    }
  }

  static Future<HouseholdStatistics> getStatistics({int? year, int? month}) async {
    final params = <String, String>{
      if (year != null) 'year': '$year',
      if (month != null) 'month': '$month',
    };
    final uri = Uri.parse('$_baseUrl/households/statistics').replace(queryParameters: params);
    final response = await http.get(uri, headers: await _authHeaders());
    if (response.statusCode == 200) {
      return HouseholdStatistics.fromJson(jsonDecode(response.body));
    } else {
      throw Exception(_errorMessage(response, 'Aile istatistiği alınamadı: ${response.statusCode}'));
    }
  }
}
