import 'receipt.dart';

/// GET /receipts/search'ten dönen sayfalı sonuç.
class ReceiptPage {
  final List<Receipt> content;
  final int page;
  final bool hasNext;
  final int totalElements;

  ReceiptPage({
    required this.content,
    required this.page,
    required this.hasNext,
    required this.totalElements,
  });

  factory ReceiptPage.fromJson(Map<String, dynamic> json) {
    return ReceiptPage(
      content: (json['content'] as List<dynamic>? ?? [])
          .map((e) => Receipt.fromJson(e as Map<String, dynamic>))
          .toList(),
      page: json['page'] as int? ?? 0,
      hasNext: json['hasNext'] as bool? ?? false,
      totalElements: json['totalElements'] as int? ?? 0,
    );
  }
}
