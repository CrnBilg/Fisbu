import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:http/http.dart' as http;
import 'package:image_picker/image_picker.dart';
import 'package:path_provider/path_provider.dart';
import '../models/receipt.dart';
import '../models/receipt_page.dart';
import '../models/receipt_item.dart';
import '../models/category.dart';
import '../models/restore_receipt_result.dart';
import '../models/spending_analysis_result.dart';
import '../models/personal_inflation_summary.dart';
import '../models/product_price_history.dart';
import '../models/store_stat.dart';
import '../models/top_product.dart';
import '../models/subscription_candidate.dart';
import '../models/imported_transaction.dart';
import '../models/parsed_statement_result.dart';
import '../models/bulk_import_result.dart';
import '../models/split_participant.dart';
import 'auth_service.dart';
import 'local_cache_service.dart';
import 'pending_receipt_queue.dart';

/// Backend'in 409 (Conflict) ile "bu fiş zaten kayıtlı" uyarısı döndüğü durumda fırlatılır.
class DuplicateReceiptException implements Exception {
  final String message;
  DuplicateReceiptException(this.message);
}

class ReceiptService {
  static const String _baseUrl = 'https://fisbu-production-613c.up.railway.app';

  static bool _isNetworkFailure(Object e) {
    return e is SocketException || e is http.ClientException;
  }

  static Future<List<Receipt>> getReceipts() async {
    try {
      final token = await AuthService.getToken();
      final response = await http.get(
        Uri.parse('$_baseUrl/receipts'),
        headers: {'Authorization': 'Bearer $token'},
      );
      if (response.statusCode == 200) {
        final List<dynamic> data = jsonDecode(response.body);
        await LocalCacheService.put(LocalCacheService.receiptsBox, 'all', data);
        return data.map((json) => Receipt.fromJson(json)).toList();
      } else {
        throw Exception('Fişler yüklenemedi: ${response.statusCode}');
      }
    } catch (e) {
      if (_isNetworkFailure(e)) {
        final cached = LocalCacheService.get(LocalCacheService.receiptsBox, 'all');
        if (cached != null) {
          return (cached as List)
              .map((json) => Receipt.fromJson(Map<String, dynamic>.from(json as Map)))
              .toList();
        }
      }
      rethrow;
    }
  }

  /// Fiş listesi ekranı: mağaza adına göre arama + kategori filtresi + sayfalama.
  /// [categoryId] ve [uncategorized] aynı anda kullanılmaz; uncategorized true ise
  /// categoryId yok sayılır.
  static Future<ReceiptPage> searchReceipts({
    String? query,
    int? categoryId,
    bool uncategorized = false,
    int page = 0,
    int size = 20,
  }) async {
    final token = await AuthService.getToken();
    final params = <String, String>{
      'page': '$page',
      'size': '$size',
    };
    if (query != null && query.trim().isNotEmpty) params['query'] = query.trim();
    if (uncategorized) {
      params['uncategorized'] = 'true';
    } else if (categoryId != null) {
      params['categoryId'] = '$categoryId';
    }

    final uri = Uri.parse('$_baseUrl/receipts/search').replace(queryParameters: params);
    final response = await http.get(uri, headers: {'Authorization': 'Bearer $token'});
    if (response.statusCode == 200) {
      return ReceiptPage.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Fişler yüklenemedi: ${response.statusCode}');
    }
  }

  /// Fotoğraf yükleme + fiş kaydını tek çağrıda yapar. Bağlantı yoksa
  /// (görsel varsa dosyayı kalıcı bir konuma kopyalayıp) her ikisini de
  /// yerel sıraya alır ve bağlantı gelince otomatik gönderilir.
  static Future<Receipt?> submitReceipt({
    required String storeName,
    required double totalAmount,
    required String receiptDate,
    int? categoryId,
    XFile? image,
    List<ReceiptItem>? items,
    bool allowDuplicate = false,
    String? returnDeadline,
    String? warrantyExpiryDate,
  }) async {
    try {
      String? imageUrl;
      if (image != null) {
        imageUrl = await uploadImage(image);
      }
      return await createReceipt(
        storeName: storeName,
        totalAmount: totalAmount,
        receiptDate: receiptDate,
        categoryId: categoryId,
        imageUrl: imageUrl,
        returnDeadline: returnDeadline,
        warrantyExpiryDate: warrantyExpiryDate,
        items: items,
        allowDuplicate: allowDuplicate,
      );
    } catch (e) {
      if (e is DuplicateReceiptException) rethrow;
      if (!_isNetworkFailure(e)) rethrow;

      String? localImagePath;
      if (image != null) {
        localImagePath = await _persistImageLocally(image);
      }

      await PendingReceiptQueue.enqueue(PendingReceipt(
        localId: DateTime.now().microsecondsSinceEpoch.toString(),
        storeName: storeName,
        totalAmount: totalAmount,
        receiptDate: receiptDate,
        categoryId: categoryId,
        localImagePath: localImagePath,
        items: items ?? const [],
      ));
      return null;
    }
  }

  static Future<String> _persistImageLocally(XFile image) async {
    final dir = await getApplicationDocumentsDirectory();
    final filename = 'pending_${DateTime.now().microsecondsSinceEpoch}_${image.name}';
    final destPath = '${dir.path}/$filename';
    await File(image.path).copy(destPath);
    return destPath;
  }

  static Future<Receipt> createReceipt({
    required String storeName,
    required double totalAmount,
    required String receiptDate,
    int? categoryId,
    String? imageUrl,
    List<ReceiptItem>? items,
    bool allowDuplicate = false,
    String? returnDeadline,
    String? warrantyExpiryDate,
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
        'allowDuplicate': allowDuplicate,
        'returnDeadline': returnDeadline,
        'warrantyExpiryDate': warrantyExpiryDate,
        if (items != null && items.isNotEmpty)
          'items': items.map((e) => e.toJson()).toList(),
      }),
    );
    if (response.statusCode == 201) {
      return Receipt.fromJson(jsonDecode(response.body));
    } else if (response.statusCode == 409) {
      String message = 'Bu fiş zaten kayıtlı görünüyor';
      try {
        message = jsonDecode(response.body)['error'] as String? ?? message;
      } catch (_) {}
      throw DuplicateReceiptException(message);
    } else {
      throw Exception('Fiş eklenemedi: ${response.statusCode}');
    }
  }

  static Future<Receipt> saveSplit(int receiptId, List<SplitParticipant> participants) async {
    final token = await AuthService.getToken();
    final response = await http.put(
      Uri.parse('$_baseUrl/receipts/$receiptId/split'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
      body: jsonEncode({
        'participants': participants.map((e) => e.toJson()).toList(),
      }),
    );
    if (response.statusCode == 200) {
      return Receipt.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Bölüştürme kaydedilemedi: ${response.statusCode}');
    }
  }

  /// Garanti/iade hatırlatıcı tarihlerini fiş eklendikten sonra kurar/günceller.
  /// null gönderilen alan temizlenir (hatırlatma kaldırılır).
  static Future<Receipt> setReminders(int receiptId, {String? returnDeadline, String? warrantyExpiryDate}) async {
    final token = await AuthService.getToken();
    final response = await http.put(
      Uri.parse('$_baseUrl/receipts/$receiptId/reminders'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
      body: jsonEncode({
        'returnDeadline': returnDeadline,
        'warrantyExpiryDate': warrantyExpiryDate,
      }),
    );
    if (response.statusCode == 200) {
      return Receipt.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Hatırlatıcı kaydedilemedi: ${response.statusCode}');
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
    try {
      final token = await AuthService.getToken();
      final response = await http.get(
        Uri.parse('$_baseUrl/categories'),
        headers: {'Authorization': 'Bearer $token'},
      );
      if (response.statusCode == 200) {
        final List<dynamic> data = jsonDecode(response.body);
        await LocalCacheService.put(LocalCacheService.categoriesBox, 'all', data);
        return data.map((json) => Category.fromJson(json)).toList();
      } else {
        throw Exception('Kategoriler yüklenemedi: ${response.statusCode}');
      }
    } catch (e) {
      if (_isNetworkFailure(e)) {
        final cached = LocalCacheService.get(LocalCacheService.categoriesBox, 'all');
        if (cached != null) {
          return (cached as List)
              .map((json) => Category.fromJson(Map<String, dynamic>.from(json as Map)))
              .toList();
        }
      }
      rethrow;
    }
  }

  /// Elle fiş eklerken mağaza adına göre kategori önerisi. Öneri yoksa null döner.
  static Future<({int categoryId, String categoryName})?> getCategorySuggestion(String storeName) async {
    if (storeName.trim().length < 2) return null;
    final token = await AuthService.getToken();
    final uri = Uri.parse('$_baseUrl/receipts/category-suggestion')
        .replace(queryParameters: {'storeName': storeName.trim()});
    final response = await http.get(uri, headers: {'Authorization': 'Bearer $token'});
    if (response.statusCode == 200) {
      final data = jsonDecode(response.body) as Map<String, dynamic>;
      return (categoryId: data['categoryId'] as int, categoryName: data['categoryName'] as String);
    }
    return null;
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

  static Future<List<StoreStat>> getStoreStatistics() async {
    final token = await AuthService.getToken();
    final response = await http.get(
      Uri.parse('$_baseUrl/statistics/stores'),
      headers: {'Authorization': 'Bearer $token'},
    );
    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => StoreStat.fromJson(json)).toList();
    } else {
      throw Exception('Mağaza istatistikleri alınamadı: ${response.statusCode}');
    }
  }

  static Future<List<TopProduct>> getTopProducts({int limit = 10}) async {
    final token = await AuthService.getToken();
    final response = await http.get(
      Uri.parse('$_baseUrl/statistics/top-products?limit=$limit'),
      headers: {'Authorization': 'Bearer $token'},
    );
    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => TopProduct.fromJson(json)).toList();
    } else {
      throw Exception('En çok alınan ürünler alınamadı: ${response.statusCode}');
    }
  }

  static Future<List<SubscriptionCandidate>> getPotentialSubscriptions() async {
    final token = await AuthService.getToken();
    final response = await http.get(
      Uri.parse('$_baseUrl/statistics/subscriptions'),
      headers: {'Authorization': 'Bearer $token'},
    );
    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => SubscriptionCandidate.fromJson(json)).toList();
    } else {
      throw Exception('Olası abonelikler alınamadı: ${response.statusCode}');
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

  /// Banka ekstresi (PDF/CSV) ya da uygulamanın kendi CSV export'unu yükleyip
  /// içindeki işlemleri harcama önerisi listesine dönüştürür. Hiçbir şey kaydetmez.
  static Future<ParsedStatementResult> importStatement(File file) async {
    final token = await AuthService.getToken();
    final request = http.MultipartRequest(
      'POST',
      Uri.parse('$_baseUrl/receipts/import/parse'),
    );
    request.headers['Authorization'] = 'Bearer $token';
    request.files.add(await http.MultipartFile.fromPath('file', file.path));
    final streamedResponse = await request.send();
    final response = await http.Response.fromStream(streamedResponse);
    if (response.statusCode == 200) {
      return ParsedStatementResult.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Ekstre okunamadı: ${response.statusCode}');
    }
  }

  /// Kullanıcının seçtiği işlemleri toplu olarak fiş olarak kaydeder.
  static Future<BulkImportResult> confirmImport(List<ImportedTransaction> transactions) async {
    final token = await AuthService.getToken();
    final response = await http.post(
      Uri.parse('$_baseUrl/receipts/import/confirm'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
      body: jsonEncode({
        'receipts': transactions
            .map((t) => {
                  'storeName': t.description,
                  'totalAmount': t.amount,
                  'receiptDate': t.date,
                  'categoryId': t.matchedCategoryId,
                })
            .toList(),
      }),
    );
    if (response.statusCode == 201) {
      return BulkImportResult.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('İçe aktarma başarısız oldu: ${response.statusCode}');
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

  /// Çevrimdışıyken sıraya alınmış bir fişin daha önce diske kopyalanmış
  /// görselini bağlantı gelince yüklemek için kullanılır.
  static Future<String> uploadImageFile(File file) async {
    final token = await AuthService.getToken();
    final request = http.MultipartRequest(
      'POST',
      Uri.parse('$_baseUrl/receipts/upload'),
    );
    request.headers['Authorization'] = 'Bearer $token';
    request.files.add(await http.MultipartFile.fromPath('file', file.path));
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