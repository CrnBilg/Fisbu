import 'dart:convert';
import 'dart:typed_data';
import 'package:http/http.dart' as http;
import 'package:image_picker/image_picker.dart';
import '../models/receipt.dart';
import '../models/receipt_item.dart';
import '../models/category.dart';
import '../models/restore_receipt_result.dart';
import '../models/spending_analysis_result.dart';
import '../models/personal_inflation_summary.dart';
import '../models/product_price_history.dart';
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
    List<ReceiptItem>? items,
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
        if (items != null && items.isNotEmpty)
          'items': items.map((e) => e.toJson()).toList(),
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

  static Future<void> deleteCategory(int id) async {
    final token = await AuthService.getToken();
    final response = await http.delete(
      Uri.parse('$_baseUrl/categories/$id'),
      headers: {'Authorization': 'Bearer $token'},
    );
    if (response.statusCode != 204) {
      throw Exception('Kategori silinemedi: ${response.statusCode}');
    }
  }

  static Future<Category> updateCategory({
    required int id,
    required String name,
    required String color,
  }) async {
    final token = await AuthService.getToken();
    final response = await http.put(
      Uri.parse('$_baseUrl/categories/$id'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
      body: jsonEncode({'name': name, 'color': color}),
    );
    if (response.statusCode == 200) {
      return Category.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Kategori güncellenemedi: ${response.statusCode}');
    }
  }

  static Future<RestoreReceiptResult> restoreReceipt(String rawOcrText) async {
    final token = await AuthService.getToken();
    final response = await http.post(
      Uri.parse('$_baseUrl/ai/restore-receipt'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
      body: jsonEncode({'rawOcrText': rawOcrText}),
    );
    if (response.statusCode == 200) {
      return RestoreReceiptResult.fromJson(jsonDecode(response.body));
    } else if (response.statusCode == 503) {
      throw Exception('AI servisi şu anda kullanılamıyor');
    } else {
      throw Exception('AI restorasyonu başarısız: ${response.statusCode}');
    }
  }

  static Future<SpendingAnalysisResult> getSpendingAnalysis({
    required int year,
    required int month,
  }) async {
    final token = await AuthService.getToken();
    final response = await http.get(
      Uri.parse('$_baseUrl/ai/spending-analysis?year=$year&month=$month'),
      headers: {'Authorization': 'Bearer $token'},
    );
    if (response.statusCode == 200) {
      return SpendingAnalysisResult.fromJson(jsonDecode(response.body));
    } else if (response.statusCode == 503) {
      throw Exception('AI servisi şu anda kullanılamıyor');
    } else {
      throw Exception('AI yorumu alınamadı: ${response.statusCode}');
    }
  }

  /// format: 'pdf' | 'excel' | 'csv'; start/end: yyyy-MM-dd
  static Future<Uint8List> exportReceipts({
    required String format,
    required String start,
    required String end,
  }) async {
    final token = await AuthService.getToken();
    final response = await http.get(
      Uri.parse('$_baseUrl/receipts/export?format=$format&start=$start&end=$end'),
      headers: {'Authorization': 'Bearer $token'},
    );
    if (response.statusCode == 200) {
      return response.bodyBytes;
    } else {
      throw Exception('Export başarısız oldu: ${response.statusCode}');
    }
  }

  static Future<PersonalInflationSummary> getInflationSummary({int months = 3}) async {
    final token = await AuthService.getToken();
    final response = await http.get(
      Uri.parse('$_baseUrl/inflation/summary?months=$months'),
      headers: {'Authorization': 'Bearer $token'},
    );
    if (response.statusCode == 200) {
      return PersonalInflationSummary.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Enflasyon özeti alınamadı: ${response.statusCode}');
    }
  }

  static Future<ProductPriceHistory> getProductPriceHistory(String normalizedName) async {
    final token = await AuthService.getToken();
    final response = await http.get(
      Uri.parse('$_baseUrl/inflation/products/${Uri.encodeComponent(normalizedName)}/history'),
      headers: {'Authorization': 'Bearer $token'},
    );
    if (response.statusCode == 200) {
      return ProductPriceHistory.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Ürün fiyat geçmişi alınamadı: ${response.statusCode}');
    }
  }

  static Future<String> uploadImage(XFile image) async {
    final token = await AuthService.getToken();
    final request = http.MultipartRequest(
      'POST',
      Uri.parse('$_baseUrl/receipts/upload'),
    );
    request.headers['Authorization'] = 'Bearer $token';
    final bytes = await image.readAsBytes();
    final multipartFile = http.MultipartFile.fromBytes(
      'file',
      bytes,
      filename: image.name,
    );
    request.files.add(multipartFile);
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