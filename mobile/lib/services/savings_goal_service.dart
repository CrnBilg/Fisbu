import 'dart:convert';
import '../models/savings_goal.dart';
import '../models/savings_goal_suggestion.dart';
import 'api_client.dart';

class SavingsGoalService {
  static Future<List<SavingsGoal>> getGoals() async {
    final response = await ApiClient.get('/savings-goals');
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
    final response = await ApiClient.post('/savings-goals',
        body: {'name': name, 'targetAmount': targetAmount, 'targetDate': targetDate});
    if (response.statusCode == 201) {
      return SavingsGoal.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Hedef oluşturulamadı: ${response.statusCode}');
    }
  }

  static Future<SavingsGoal> contribute(int goalId, double amount) async {
    final response = await ApiClient.put('/savings-goals/$goalId/contribute', body: {'amount': amount});
    if (response.statusCode == 200) {
      return SavingsGoal.fromJson(jsonDecode(response.body));
    } else {
      throw Exception(ApiClient.errorMessage(response, fallback: 'İşlem yapılamadı'));
    }
  }

  static Future<SavingsGoalSuggestion> getSuggestion(int goalId) async {
    final response = await ApiClient.get('/savings-goals/$goalId/suggestion');
    if (response.statusCode == 200) {
      return SavingsGoalSuggestion.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Öneri alınamadı: ${response.statusCode}');
    }
  }

  static Future<void> deleteGoal(int goalId) async {
    final response = await ApiClient.delete('/savings-goals/$goalId');
    if (response.statusCode != 204) {
      throw Exception('Hedef silinemedi: ${response.statusCode}');
    }
  }
}
