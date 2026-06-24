import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:image_picker/image_picker.dart';
import '../models/receipt.dart';
import '../models/category.dart';
import 'auth_service.dart';

class ReceiptService {
  static const String _baseUrl = 'https://fisbu-production-613c.up.railway.app';

  static Future<List<Receipt>> getReceipts() async {
    final token = await AuthService.getToken();
    final response = await http.get(
      Uri.parse('$_baseUrl/receipts'),
      headers: {'Authorization': 'Bearer $token'},
    );
    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => Receipt.fromJson(json)).toList();
    } else {
      throw Exception('Fişler yüklenemedi: ${response.statusCode}');
    }
  }

  static Future<Receipt> createReceipt({
    required String storeName,
    required double totalAmount,
    required String receiptDate,
    int? categoryId,
    String? imageUrl,
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
        'imageUrl': imageUrl,
      }),
    );
    if (response.statusCode == 201) {
      return Receipt.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Fiş eklenemedi: ${response.statusCode}');
    }
  }

  static Future<void> deleteReceipt(int id) async {
    final token = await AuthService.getToken();
    final response = await http.delete(
      Uri.parse('$_baseUrl/receipts/$id'),
      headers: {'Authorization': 'Bearer $token'},
    );
    if (response.statusCode != 204) {
      throw Exception('Fiş silinemedi: ${response.statusCode}');
    }
  }

  static Future<List<Category>> getCategories() async {
    final token = await AuthService.getToken();
    final response = await http.get(
      Uri.parse('$_baseUrl/categories'),
      headers: {'Authorization': 'Bearer $token'},
    );
    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => Category.fromJson(json)).toList();
    } else {
      throw Exception('Kategoriler yüklenemedi: ${response.statusCode}');
    }
  }

  static Future<Category> createCategory({
    required String name,
    required String color,
  }) async {
    final token = await AuthService.getToken();
    final response = await http.post(
      Uri.parse('$_baseUrl/categories'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
      body: jsonEncode({'name': name, 'color': color}),
    );
    if (response.statusCode == 201) {
      return Category.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Kategori eklenemedi: ${response.statusCode}');
    }
  }

  static Future<String> uploadImage(XFile image) async {
    final token = await AuthService.getToken();
    final request = http.MultipartRequest(
      'POST',
      Uri.parse('$_baseUrl/receipts/upload'),
    );
    request.headers['Authorization'] = 'Bearer $token';
    request.files.add(await http.MultipartFile.fromPath('file', image.path));
    final streamedResponse = await request.send();
    final response = await http.Response.fromStream(streamedResponse);
    if (response.statusCode == 200) {
      final data = jsonDecode(response.body) as Map<String, dynamic>;
      return data['imageUrl'] as String;
    } else {
      throw Exception('Fotoğraf yüklenemedi: ${response.statusCode}');
    }
  }
}