import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import '../models/budget.dart';
import '../models/budget_suggestion.dart';
import 'auth_service.dart';
import 'local_cache_service.dart';

class BudgetService {
  static const String _baseUrl = 'https://fisbu-production-613c.up.railway.app';

  static Future<List<Budget>> getBudgets({int? year, int? month}) async {
    try {
      final token = await AuthService.getToken();
      final query = <String, String>{
        if (year != null) 'year': '$year',
        if (month != null) 'month': '$month',
      };
      final uri = Uri.parse('$_baseUrl/budgets')
          .replace(queryParameters: query.isEmpty ? null : query);
      final response = await http.get(uri, headers: {'Authorization': 'Bearer $token'});
      if (response.statusCode == 200) {
        final List<dynamic> data = jsonDecode(response.body);
        // Sadece filtresiz (tüm bütçeler) sorguyu cache'liyoruz — dashboard'un offline fallback ihtiyacı bu
        if (year == null && month == null) {
          await LocalCacheService.put(LocalCacheService.budgetsBox, 'all', data);
        }
        return data.map((json) => Budget.fromJson(json)).toList();
      } else {
        throw Exception('Bütçeler yüklenemedi: ${response.statusCode}');
      }
    } catch (e) {
      if ((e is SocketException || e is http.ClientException) && year == null && month == null) {
        final cached = LocalCacheService.get(LocalCacheService.budgetsBox, 'all');
        if (cached != null) {
          return (cached as List)
              .map((json) => Budget.fromJson(Map<String, dynamic>.from(json as Map)))
              .toList();
        }
      }
      rethrow;
    }
  }

  static Future<Budget> createBudget({
    required int categoryId,
    required double monthlyLimit,
    required int year,
    required int month,
  }) async {
    final token = await AuthService.getToken();
    final response = await http.post(
      Uri.parse('$_baseUrl/budgets'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
      body: jsonEncode({
        'categoryId': categoryId,
        'monthlyLimit': monthlyLimit,
        'year': year,
        'month': month,
      }),
    );
    if (response.statusCode == 201) {
      return Budget.fromJson(jsonDecode(response.body));
    } else if (response.statusCode == 409) {
      throw Exception('Bu kategori için bu ay zaten bir bütçe var');
    } else {
      throw Exception('Bütçe eklenemedi: ${response.statusCode}');
    }
  }

  static Future<Budget> updateBudget({
    required int id,
    required int categoryId,
    required double monthlyLimit,
    required int year,
    required int month,
  }) async {
    final token = await AuthService.getToken();
    final response = await http.put(
      Uri.parse('$_baseUrl/budgets/$id'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
      body: jsonEncode({
        'categoryId': categoryId,
        'monthlyLimit': monthlyLimit,
        'year': year,
        'month': month,
      }),
    );
    if (response.statusCode == 200) {
      return Budget.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Bütçe güncellenemedi: ${response.statusCode}');
    }
  }

  static Future<BudgetSuggestion> getBudgetSuggestion({
    required int categoryId,
    int? year,
    int? month,
  }) async {
    final token = await AuthService.getToken();
    final query = <String, String>{
      'categoryId': '$categoryId',
      if (year != null) 'year': '$year',
      if (month != null) 'month': '$month',
    };
    final uri = Uri.parse('$_baseUrl/budgets/suggestion').replace(queryParameters: query);
    final response = await http.get(uri, headers: {'Authorization': 'Bearer $token'});
    if (response.statusCode == 200) {
      return BudgetSuggestion.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Bütçe önerisi alınamadı: ${response.statusCode}');
    }
  }

  static Future<void> deleteBudget(int id) async {
    final token = await AuthService.getToken();
    final response = await http.delete(
      Uri.parse('$_baseUrl/budgets/$id'),
      headers: {'Authorization': 'Bearer $token'},
    );
    if (response.statusCode != 204) {
      throw Exception('Bütçe silinemedi: ${response.statusCode}');
    }
  }
}
