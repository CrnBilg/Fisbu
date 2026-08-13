import 'receipt_item.dart';
import 'split_participant.dart';

class Receipt {
  final int id;
  final String storeName;
  final double totalAmount;
  final String receiptDate;
  final int? categoryId;
  final String? categoryName;
  final String? imageUrl;
  final String? createdAt;
  final List<ReceiptItem> items;
  final List<SplitParticipant>? splitParticipants;
  final String? returnDeadline;
  final String? warrantyExpiryDate;
  final String? anomalyWarning;

  Receipt({
    required this.id,
    required this.storeName,
    required this.totalAmount,
    required this.receiptDate,
    this.categoryId,
    this.categoryName,
    this.imageUrl,
    this.createdAt,
    this.items = const [],
    this.splitParticipants,
    this.returnDeadline,
    this.warrantyExpiryDate,
    this.anomalyWarning,
  });

  factory Receipt.fromJson(Map<String, dynamic> json) {
    // id/totalAmount zorunlu alanlar — backend kısmi/bozuk bir yanıt dönerse
    // ham `as int`/`as num` cast'i belirsiz bir TypeError fırlatırdı; bunun yerine
    // net bir FormatException fırlatıyoruz (çağıranların mevcut catch(e) +
    // NetworkError.friendlyMessage akışı bunu zaten düzgün bir mesaja çeviriyor)
    final id = json['id'];
    final totalAmount = json['totalAmount'];
    if (id is! int) {
      throw const FormatException('Fiş verisi geçersiz: id alanı eksik/bozuk');
    }
    if (totalAmount is! num) {
      throw const FormatException('Fiş verisi geçersiz: totalAmount alanı eksik/bozuk');
    }
    return Receipt(
      id: id,
      storeName: json['storeName'] as String? ?? '',
      totalAmount: totalAmount.toDouble(),
      receiptDate: json['receiptDate'] as String? ?? '',
      categoryId: json['categoryId'] as int?,
      categoryName: json['categoryName'] as String?,
      imageUrl: json['imageUrl'] as String?,
      createdAt: json['createdAt'] as String?,
      items: (json['items'] as List<dynamic>?)
              ?.map((e) => ReceiptItem.fromJson(e as Map<String, dynamic>))
              .toList() ??
          const [],
      splitParticipants: (json['splitParticipants'] as List<dynamic>?)
          ?.map((e) => SplitParticipant.fromJson(e as Map<String, dynamic>))
          .toList(),
      returnDeadline: json['returnDeadline'] as String?,
      warrantyExpiryDate: json['warrantyExpiryDate'] as String?,
      anomalyWarning: json['anomalyWarning'] as String?,
    );
  }
}