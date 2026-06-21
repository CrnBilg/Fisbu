import 'dart:convert'; // JSON işlemleri için gerekli kütüphane
import 'package:http/http.dart' as http; // HTTP istekleri için gerekli kütüphane
import '../models/receipt.dart'; // Fiş modelini içe aktarır
import '../models/category.dart'; // Kategori modelini içe aktarır
import 'auth_service.dart'; // Kimlik doğrulama işlemleri için gerekli servis

class ReceiptService {
  static const String _baseUrl = 'https://fisbu-production-613c.up.railway.app'; // API'nin temel URL'si

  // Tüm fişleri listeler
  static Future<List<Receipt>> getReceipts() async { // Kimlik doğrulama tokeni alir
    final token = await AuthService.getToken(); // API'ye GET isteği gönderiyoruz

    final response = await http.get( // API'ye GET isteği gönderiyoruz
      Uri.parse('$_baseUrl/receipts'), //
      headers: {
        'Authorization': 'Bearer $token',
      },
    );

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => Receipt.fromJson(json)).toList();
    } else {
      throw Exception('Fişler yüklenemedi: ${response.statusCode}');
    }
  }

  // Yeni fiş ekler
  static Future<Receipt> createReceipt({
    required String storeName,
    required double totalAmount,
    required String receiptDate,
    int? categoryId,
  }) async {
    final token = await AuthService.getToken();

    final response = await http.post(
      Uri.parse('$_baseUrl/receipts'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
      body: jsonEncode({
        'storeName': storeName,
        'totalAmount': totalAmount,
        'receiptDate': receiptDate,
        'categoryId': categoryId,
      }),
    );

    if (response.statusCode == 201) {
      return Receipt.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Fiş eklenemedi: ${response.statusCode}');
    }
  }

  // Fiş siler
  static Future<void> deleteReceipt(int id) async {
    final token = await AuthService.getToken();

    final response = await http.delete(
      Uri.parse('$_baseUrl/receipts/$id'),
      headers: {
        'Authorization': 'Bearer $token',
      },
    );

    if (response.statusCode != 204) {
      throw Exception('Fiş silinemedi: ${response.statusCode}');
    }
  }

  // Kategorileri listeler
  static Future<List<Category>> getCategories() async {
    final token = await AuthService.getToken();

    final response = await http.get(
      Uri.parse('$_baseUrl/categories'),
      headers: {
        'Authorization': 'Bearer $token',
      },
    );

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => Category.fromJson(json)).toList();
    } else {
      throw Exception('Kategoriler yüklenemedi: ${response.statusCode}');
    }
  }
}