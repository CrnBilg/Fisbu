import 'dart:convert';
import '../models/household.dart';
import '../models/household_statistics.dart';
import 'api_client.dart';

class HouseholdService {
  /// Kullanıcı bir aileye üye değilse null döner (404 hata değil, normal durum).
  static Future<Household?> getMyHousehold() async {
    final response = await ApiClient.get('/households/me');
    if (response.statusCode == 200) {
      return Household.fromJson(jsonDecode(response.body));
    } else if (response.statusCode == 404) {
      return null;
    } else {
      throw Exception(ApiClient.errorMessage(response, fallback: 'Aile bilgisi alınamadı: ${response.statusCode}'));
    }
  }

  static Future<Household> createHousehold(String name) async {
    final response = await ApiClient.post('/households', body: {'name': name});
    if (response.statusCode == 201) {
      return Household.fromJson(jsonDecode(response.body));
    } else {
      throw Exception(ApiClient.errorMessage(response, fallback: 'Aile oluşturulamadı: ${response.statusCode}'));
    }
  }

  static Future<Household> joinHousehold(String inviteCode) async {
    final response = await ApiClient.post('/households/join', body: {'inviteCode': inviteCode});
    if (response.statusCode == 200) {
      return Household.fromJson(jsonDecode(response.body));
    } else {
      throw Exception(ApiClient.errorMessage(response, fallback: 'Aileye katılınamadı: ${response.statusCode}'));
    }
  }

  static Future<void> leaveHousehold() async {
    final response = await ApiClient.delete('/households/me');
    if (response.statusCode != 204) {
      throw Exception(ApiClient.errorMessage(response, fallback: 'Aileden ayrılınamadı: ${response.statusCode}'));
    }
  }

  static Future<HouseholdStatistics> getStatistics({int? year, int? month}) async {
    final params = <String, String>{
      if (year != null) 'year': '$year',
      if (month != null) 'month': '$month',
    };
    final response = await ApiClient.get('/households/statistics', query: params);
    if (response.statusCode == 200) {
      return HouseholdStatistics.fromJson(jsonDecode(response.body));
    } else {
      throw Exception(ApiClient.errorMessage(response, fallback: 'Aile istatistiği alınamadı: ${response.statusCode}'));
    }
  }
}
