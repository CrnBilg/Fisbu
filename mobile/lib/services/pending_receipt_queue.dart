import 'dart:io';
import 'package:hive_flutter/hive_flutter.dart';
import '../models/receipt_item.dart';
import 'connectivity_service.dart';
import 'local_cache_service.dart';
import 'receipt_service.dart';

class PendingReceipt {
  final String localId;
  final String storeName;
  final double totalAmount;
  final String receiptDate;
  final int? categoryId;
  final String? imageUrl;
  final String? localImagePath;
  final List<ReceiptItem> items;

  PendingReceipt({
    required this.localId,
    required this.storeName,
    required this.totalAmount,
    required this.receiptDate,
    this.categoryId,
    this.imageUrl,
    this.localImagePath,
    this.items = const [],
  });

  Map<String, dynamic> toMap() => {
        'localId': localId,
        'storeName': storeName,
        'totalAmount': totalAmount,
        'receiptDate': receiptDate,
        'categoryId': categoryId,
        'imageUrl': imageUrl,
        'localImagePath': localImagePath,
        'items': items.map((e) => e.toJson()).toList(),
      };

  factory PendingReceipt.fromMap(Map<dynamic, dynamic> map) {
    return PendingReceipt(
      localId: map['localId'] as String,
      storeName: map['storeName'] as String,
      totalAmount: (map['totalAmount'] as num).toDouble(),
      receiptDate: map['receiptDate'] as String,
      categoryId: map['categoryId'] as int?,
      imageUrl: map['imageUrl'] as String?,
      localImagePath: map['localImagePath'] as String?,
      items: ((map['items'] as List?) ?? const [])
          .map((e) => ReceiptItem.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList(),
    );
  }
}

/// İnternet olmadan eklenen fişleri sıraya alır; bağlantı gelince sırayla
/// gönderir. Fişler tekil/oluşturma-amaçlı olduğu için çakışma yönetimi yok —
/// sıradaki her öğe bağımsız denenir, hata durumunda flush durur.
class PendingReceiptQueue {
  static bool _isFlushing = false;

  static Box get _box => Hive.box(LocalCacheService.pendingReceiptsBox);

  static Future<void> enqueue(PendingReceipt receipt) async {
    await _box.put(receipt.localId, receipt.toMap());
  }

  static List<PendingReceipt> getAll() {
    return _box.values.map((v) => PendingReceipt.fromMap(v as Map)).toList();
  }

  static int get pendingCount => _box.length;

  static Future<void> remove(String localId) async {
    await _box.delete(localId);
  }

  static Future<void> flush() async {
    if (_isFlushing) return;
    _isFlushing = true;
    try {
      for (final pending in getAll()) {
        if (!ConnectivityService.isOnline.value) break;
        try {
          String? imageUrl = pending.imageUrl;
          if (imageUrl == null && pending.localImagePath != null) {
            final file = File(pending.localImagePath!);
            if (await file.exists()) {
              imageUrl = await ReceiptService.uploadImageFile(file);
            }
          }
          await ReceiptService.createReceipt(
            storeName: pending.storeName,
            totalAmount: pending.totalAmount,
            receiptDate: pending.receiptDate,
            categoryId: pending.categoryId,
            imageUrl: imageUrl,
            items: pending.items,
          );
          await remove(pending.localId);
          if (pending.localImagePath != null) {
            final file = File(pending.localImagePath!);
            if (await file.exists()) await file.delete();
          }
        } on DuplicateReceiptException {
          // Bu fiş zaten (ör. manuel olarak) eklenmiş — sırada tutmaya gerek yok,
          // kaldırıp kuyruktaki diğer öğelerin işlenmesine devam et
          await remove(pending.localId);
        } catch (e) {
          // Hâlâ çevrimdışıysak ya da geçici bir hata varsa dur, kalanları sırada bırak
          break;
        }
      }
    } finally {
      _isFlushing = false;
    }
  }
}
