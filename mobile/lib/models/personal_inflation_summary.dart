class ProductInflation {
  final String normalizedName;
  final String displayName;
  final double firstPrice;
  final double lastPrice;
  final double changePercent;

  ProductInflation({
    required this.normalizedName,
    required this.displayName,
    required this.firstPrice,
    required this.lastPrice,
    required this.changePercent,
  });

  factory ProductInflation.fromJson(Map<String, dynamic> json) {
    return ProductInflation(
      normalizedName: json['normalizedName'] as String? ?? '',
      displayName: json['displayName'] as String? ?? '',
      firstPrice: (json['firstPrice'] as num?)?.toDouble() ?? 0,
      lastPrice: (json['lastPrice'] as num?)?.toDouble() ?? 0,
      changePercent: (json['changePercent'] as num?)?.toDouble() ?? 0,
    );
  }
}

class PersonalInflationSummary {
  final int months;
  final int trackedProductCount;
  final double? personalInflationPercent;
  final List<ProductInflation> topIncreasing;
  final List<ProductInflation> topDecreasing;

  PersonalInflationSummary({
    required this.months,
    required this.trackedProductCount,
    this.personalInflationPercent,
    required this.topIncreasing,
    required this.topDecreasing,
  });

  factory PersonalInflationSummary.fromJson(Map<String, dynamic> json) {
    return PersonalInflationSummary(
      months: json['months'] as int? ?? 3,
      trackedProductCount: json['trackedProductCount'] as int? ?? 0,
      personalInflationPercent: (json['personalInflationPercent'] as num?)?.toDouble(),
      topIncreasing: (json['topIncreasing'] as List<dynamic>? ?? [])
          .map((e) => ProductInflation.fromJson(e as Map<String, dynamic>))
          .toList(),
      topDecreasing: (json['topDecreasing'] as List<dynamic>? ?? [])
          .map((e) => ProductInflation.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}
