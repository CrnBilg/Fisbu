class RestoreReceiptResult {
  final String? storeName;
  final double? totalAmount;
  final String? receiptDate;
  final String? suggestedCategoryName;
  final int? matchedCategoryId;
  final int confidenceScore;

  RestoreReceiptResult({
    this.storeName,
    this.totalAmount,
    this.receiptDate,
    this.suggestedCategoryName,
    this.matchedCategoryId,
    required this.confidenceScore,
  });

  factory RestoreReceiptResult.fromJson(Map<String, dynamic> json) {
    return RestoreReceiptResult(
      storeName: json['storeName'] as String?,
      totalAmount: (json['totalAmount'] as num?)?.toDouble(),
      receiptDate: json['receiptDate'] as String?,
      suggestedCategoryName: json['suggestedCategoryName'] as String?,
      matchedCategoryId: json['matchedCategoryId'] as int?,
      confidenceScore: (json['confidenceScore'] as num?)?.toInt() ?? 0,
    );
  }
}
