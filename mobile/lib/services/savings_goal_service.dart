import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/savings_goal.dart';
import '../models/savings_goal_suggestion.dart';
import 'auth_service.dart';

class SavingsGoalService {
  static const String _baseUrl = 'https://fisbu-production-613c.up.railway.app';

  static Future<Map<String, String>> _headers() async {
    final token = await AuthService.getToken();
    return {'Content-Type': 'application/json', 'Authorization': 'Bearer $token'};
  }

  static Future<List<SavingsGoal>> getGoals() async {
    final response = await http.get(Uri.parse('$_baseUrl/savings-goals'), headers: await _headers());
    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => SavingsGoal.fromJson(json)).toList();
    } else {
      throw Exception('Hedefler alınamadı: ${response.statusCode}');
    }
  }

  static Future<SavingsGoal> createGoal({
    required String name,
    required double targetAmount,
    String? targetDate,
  }) async {
    final response = await http.post(
      Uri.parse('$_baseUrl/savings-goals'),
      headers: await _headers(),
      body: jsonEncode({'name': name, 'targetAmount': targetAmount, 'targetDate': targetDate}),
    );
    if (response.statusCode == 201) {
      return SavingsGoal.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Hedef oluşturulamadı: ${response.statusCode}');
    }
  }

  static Future<SavingsGoal> contribute(int goalId, double amount) async {
    final response = await http.put(
      Uri.parse('$_baseUrl/savings-goals/$goalId/contribute'),
      headers: await _headers(),
      body: jsonEncode({'amount': amount}),
    );
    if (response.statusCode == 200) {
      return SavingsGoal.fromJson(jsonDecode(response.body));
    } else {
      String message = 'İşlem yapılamadı';
      try {
        message = jsonDecode(response.body)['error'] as String? ?? message;
      } catch (_) {}
      throw Exception(message);
    }
  }

  static Future<SavingsGoalSuggestion> getSuggestion(int goalId) async {
    final response =
        await http.get(Uri.parse('$_baseUrl/savings-goals/$goalId/suggestion'), headers: await _headers());
    if (response.statusCode == 200) {
      return SavingsGoalSuggestion.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Öneri alınamadı: ${response.statusCode}');
    }
  }

  static Future<void> deleteGoal(int goalId) async {
    final response = await http.delete(Uri.parse('$_baseUrl/savings-goals/$goalId'), headers: await _headers());
    if (response.statusCode != 204) {
      throw Exception('Hedef silinemedi: ${response.statusCode}');
    }
  }
}
