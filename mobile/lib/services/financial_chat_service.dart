import 'dart:convert';
import '../models/chat_message.dart';
import 'api_client.dart';

class FinancialChatService {
  static Future<String> sendMessage(String message, List<ChatMessage> history) async {
    final response = await ApiClient.post('/ai/chat', body: {
      'message': message,
      'history': history.map((m) => m.toJson()).toList(),
    });
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
