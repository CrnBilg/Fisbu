import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';
import 'package:image_picker/image_picker.dart';
import 'package:mime/mime.dart';
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
import '../models/spending_personality.dart';
import '../models/imported_transaction.dart';
import '../models/parsed_statement_result.dart';
import '../models/bulk_import_result.dart';
import '../models/split_participant.dart';
import 'api_client.dart';
import 'local_cache_service.dart';
import 'pending_receipt_queue.dart';

/// Backend'in 409 (Conflict) ile "bu fiş zaten kayıtlı" uyarısı döndüğü durumda fırlatılır.
class DuplicateReceiptException implements Exception {
  final String message;
  DuplicateReceiptException(this.message);
}

class ReceiptService {
  static bool _isNetworkFailure(Object e) {
    return e is SocketException || e is http.ClientException;
  }

  static Future<List<Receipt>> getReceipts() async {
    try {
      final response = await ApiClient.get('/receipts');
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

    final response = await ApiClient.get('/receipts/search', query: params);
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
    final response = await ApiClient.post('/receipts', body: {
      'storeName': storeName,
      'totalAmount': totalAmount,
      'receiptDate': receiptDate,
      'categoryId': categoryId,
      'imageUrl': imageUrl,
      'allowDuplicate': allowDuplicate,
      'returnDeadline': returnDeadline,
      'warrantyExpiryDate': warrantyExpiryDate,
      if (items != null && items.isNotEmpty) 'items': items.map((e) => e.toJson()).toList(),
    });
    if (response.statusCode == 201) {
      return Receipt.fromJson(jsonDecode(response.body));
    } else if (response.statusCode == 409) {
      String message = 'Bu fiş zaten kayıtlı görünüyor';
      try {
        message = jsonDecode(response.body)['error'] as String? ?? message;
      } catch (_) {}
      throw DuplicateReceiptException(message);
    } else {
      String message = 'Fiş eklenemedi (${response.statusCode})';
      try {
        message = jsonDecode(response.body)['error'] as String? ?? message;
      } catch (_) {}
      throw Exception(message);
    }
  }

  static Future<Receipt> saveSplit(int receiptId, List<SplitParticipant> participants) async {
    final response = await ApiClient.put('/receipts/$receiptId/split', body: {
      'participants': participants.map((e) => e.toJson()).toList(),
    });
    if (response.statusCode == 200) {
      return Receipt.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Bölüştürme kaydedilemedi: ${response.statusCode}');
    }
  }

  /// Garanti/iade hatırlatıcı tarihlerini fiş eklendikten sonra kurar/günceller.
  /// null gönderilen alan temizlenir (hatırlatma kaldırılır).
  static Future<Receipt> setReminders(int receiptId, {String? returnDeadline, String? warrantyExpiryDate}) async {
    final response = await ApiClient.put('/receipts/$receiptId/reminders', body: {
      'returnDeadline': returnDeadline,
      'warrantyExpiryDate': warrantyExpiryDate,
    });
    if (response.statusCode == 200) {
      return Receipt.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Hatırlatıcı kaydedilemedi: ${response.statusCode}');
    }
  }

  static Future<void> deleteReceipt(int id) async {
    final response = await ApiClient.delete('/receipts/$id');
    if (response.statusCode != 204) {
      throw Exception('Fiş silinemedi: ${response.statusCode}');
    }
  }

  static Future<List<Category>> getCategories() async {
    try {
      final response = await ApiClient.get('/categories');
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
    final response =
        await ApiClient.get('/receipts/category-suggestion', query: {'storeName': storeName.trim()});
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
    final response = await ApiClient.post('/categories', body: {'name': name, 'color': color});
    if (response.statusCode == 201) {
      return Category.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Kategori eklenemedi: ${response.statusCode}');
    }
  }

  static Future<void> deleteCategory(int id) async {
    final response = await ApiClient.delete('/categories/$id');
    if (response.statusCode != 204) {
      throw Exception('Kategori silinemedi: ${response.statusCode}');
    }
  }

  static Future<Category> updateCategory({
    required int id,
    required String name,
    required String color,
  }) async {
    final response = await ApiClient.put('/categories/$id', body: {'name': name, 'color': color});
    if (response.statusCode == 200) {
      return Category.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Kategori güncellenemedi: ${response.statusCode}');
    }
  }

  static Future<RestoreReceiptResult> restoreReceipt(String rawOcrText) async {
    final response = await ApiClient.post('/ai/restore-receipt', body: {'rawOcrText': rawOcrText});
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
    final response =
        await ApiClient.get('/ai/spending-analysis', query: {'year': '$year', 'month': '$month'});
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
    final response =
        await ApiClient.get('/receipts/export', query: {'format': format, 'start': start, 'end': end});
    if (response.statusCode == 200) {
      return response.bodyBytes;
    } else {
      throw Exception('Export başarısız oldu: ${response.statusCode}');
    }
  }

  static Future<PersonalInflationSummary> getInflationSummary({int months = 3}) async {
    final response = await ApiClient.get('/inflation/summary', query: {'months': '$months'});
    if (response.statusCode == 200) {
      return PersonalInflationSummary.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Enflasyon özeti alınamadı: ${response.statusCode}');
    }
  }

  static Future<List<StoreStat>> getStoreStatistics() async {
    final response = await ApiClient.get('/statistics/stores');
    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => StoreStat.fromJson(json)).toList();
    } else {
      throw Exception('Mağaza istatistikleri alınamadı: ${response.statusCode}');
    }
  }

  static Future<List<TopProduct>> getTopProducts({int limit = 10}) async {
    final response = await ApiClient.get('/statistics/top-products', query: {'limit': '$limit'});
    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => TopProduct.fromJson(json)).toList();
    } else {
      throw Exception('En çok alınan ürünler alınamadı: ${response.statusCode}');
    }
  }

  static Future<List<SubscriptionCandidate>> getPotentialSubscriptions() async {
    final response = await ApiClient.get('/statistics/subscriptions');
    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => SubscriptionCandidate.fromJson(json)).toList();
    } else {
      throw Exception('Olası abonelikler alınamadı: ${response.statusCode}');
    }
  }

  static Future<SpendingPersonality> getSpendingPersonality() async {
    final response = await ApiClient.get('/statistics/spending-personality');
    if (response.statusCode == 200) {
      return SpendingPersonality.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Harcama kişiliği alınamadı: ${response.statusCode}');
    }
  }

  static Future<ProductPriceHistory> getProductPriceHistory(String normalizedName) async {
    final response =
        await ApiClient.get('/inflation/products/${Uri.encodeComponent(normalizedName)}/history');
    if (response.statusCode == 200) {
      return ProductPriceHistory.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Ürün fiyat geçmişi alınamadı: ${response.statusCode}');
    }
  }

  /// Banka ekstresi (PDF/CSV) ya da uygulamanın kendi CSV export'unu yükleyip
  /// içindeki işlemleri harcama önerisi listesine dönüştürür. Hiçbir şey kaydetmez.
  static Future<ParsedStatementResult> importStatement(File file) async {
    final response = await ApiClient.postMultipart(
      '/receipts/import/parse',
      file: await http.MultipartFile.fromPath('file', file.path),
    );
    if (response.statusCode == 200) {
      return ParsedStatementResult.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Ekstre okunamadı: ${response.statusCode}');
    }
  }

  /// Kullanıcının seçtiği işlemleri toplu olarak fiş olarak kaydeder.
  static Future<BulkImportResult> confirmImport(List<ImportedTransaction> transactions) async {
    final response = await ApiClient.post('/receipts/import/confirm', body: {
      'receipts': transactions
          .map((t) => {
                'storeName': t.description,
                'totalAmount': t.amount,
                'receiptDate': t.date,
                'categoryId': t.matchedCategoryId,
              })
          .toList(),
    });
    if (response.statusCode == 201) {
      return BulkImportResult.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('İçe aktarma başarısız oldu: ${response.statusCode}');
    }
  }

  static Future<String> uploadImage(XFile image) async {
    final bytes = await image.readAsBytes();
    // http.MultipartFile.fromBytes contentType belirtilmezse application/octet-stream'e
    // düşer — backend'in content-type doğrulaması bunu reddeder, o yüzden dosya
    // baytlarından (uzantı yoksa/yanlışsa bile güvenilir) MIME türünü açıkça belirliyoruz
    final mimeType = lookupMimeType(image.name, headerBytes: bytes) ?? 'image/jpeg';
    final response = await ApiClient.postMultipart(
      '/receipts/upload',
      file: http.MultipartFile.fromBytes('file', bytes, filename: image.name, contentType: MediaType.parse(mimeType)),
    );
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
    final mimeType = lookupMimeType(file.path) ?? 'image/jpeg';
    final response = await ApiClient.postMultipart(
      '/receipts/upload',
      file: await http.MultipartFile.fromPath('file', file.path, contentType: MediaType.parse(mimeType)),
    );
    if (response.statusCode == 200) {
      final data = jsonDecode(response.body) as Map<String, dynamic>;
      return data['imageUrl'] as String;
    } else {
      throw Exception('Fotoğraf yüklenemedi: ${response.statusCode}');
    }
  }
}