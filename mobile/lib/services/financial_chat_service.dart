import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/chat_message.dart';
import 'auth_service.dart';

class FinancialChatService {
  static const String _baseUrl = 'https://fisbu-production-613c.up.railway.app';

  static Future<String> sendMessage(String message, List<ChatMessage> history) async {
    final token = await AuthService.getToken();
    final response = await http.post(
      Uri.parse('$_baseUrl/ai/chat'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
      body: jsonEncode({
        'message': message,
        'history': history.map((m) => m.toJson()).toList(),
      }),
    );
    if (response.statusCode == 200) {
      final data = jsonDecode(response.body) as Map<String, dynamic>;
      return data['reply'] as String? ?? '';
    } else if (response.statusCode == 503) {
      throw Exception('AI servisi şu anda kullanılamıyor');
    } else {
      throw Exception('Yanıt alınamadı: ${response.statusCode}');
    }
  }
}
