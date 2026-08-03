class TopProduct {
  final String normalizedName;
  final String displayName;
  final int purchaseCount;
  final double totalSpent;

  TopProduct({
    required this.normalizedName,
    required this.displayName,
    required this.purchaseCount,
    required this.totalSpent,
  });

  factory TopProduct.fromJson(Map<String, dynamic> json) {
    return TopProduct(
      normalizedName: json['normalizedName'] as String? ?? '',
      displayName: json['displayName'] as String? ?? '',
      purchaseCount: json['purchaseCount'] as int? ?? 0,
      totalSpent: (json['totalSpent'] as num?)?.toDouble() ?? 0,
    );
  }
}
