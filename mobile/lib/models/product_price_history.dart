class ProductPricePoint {
  final String date;
  final double unitPrice;
  final String? storeName;

  ProductPricePoint({required this.date, required this.unitPrice, this.storeName});

  factory ProductPricePoint.fromJson(Map<String, dynamic> json) {
    return ProductPricePoint(
      date: json['date'] as String? ?? '',
      unitPrice: (json['unitPrice'] as num?)?.toDouble() ?? 0,
      storeName: json['storeName'] as String?,
    );
  }
}

class ProductPriceHistory {
  final String normalizedName;
  final String displayName;
  final List<ProductPricePoint> points;

  ProductPriceHistory({
    required this.normalizedName,
    required this.displayName,
    required this.points,
  });

  factory ProductPriceHistory.fromJson(Map<String, dynamic> json) {
    return ProductPriceHistory(
      normalizedName: json['normalizedName'] as String? ?? '',
      displayName: json['displayName'] as String? ?? '',
      points: (json['points'] as List<dynamic>? ?? [])
          .map((e) => ProductPricePoint.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}
