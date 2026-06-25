class Receipt {
  final int id;
  final String storeName;
  final double totalAmount;
  final String receiptDate;
  final int? categoryId;
  final String? categoryName;
  final String? imageUrl;
  final String? createdAt;

  Receipt({
    required this.id,
    required this.storeName,
    required this.totalAmount,
    required this.receiptDate,
    this.categoryId,
    this.categoryName,
    this.imageUrl,
    this.createdAt,
  });

  factory Receipt.fromJson(Map<String, dynamic> json) {
    return Receipt(
      id: json['id'] as int,
      storeName: json['storeName'] as String? ?? '',
      totalAmount: (json['totalAmount'] as num).toDouble(),
      receiptDate: json['receiptDate'] as String? ?? '',
      categoryId: json['categoryId'] as int?,
      categoryName: json['categoryName'] as String?,
      imageUrl: json['imageUrl'] as String?,
      createdAt: json['createdAt'] as String?,
    );
  }
}