class StoreStat {
  final String storeName;
  final double totalAmount;
  final double averageAmount;
  final int receiptCount;

  StoreStat({
    required this.storeName,
    required this.totalAmount,
    required this.averageAmount,
    required this.receiptCount,
  });

  factory StoreStat.fromJson(Map<String, dynamic> json) {
    return StoreStat(
      storeName: json['storeName'] as String? ?? 'Diğer',
      totalAmount: (json['totalAmount'] as num?)?.toDouble() ?? 0,
      averageAmount: (json['averageAmount'] as num?)?.toDouble() ?? 0,
      receiptCount: json['receiptCount'] as int? ?? 0,
    );
  }
}
