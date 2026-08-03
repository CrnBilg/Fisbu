class BudgetSuggestion {
  final int categoryId;
  final String categoryName;
  final int monthsAnalyzed;
  final double? averageSpend;
  final double? suggestedLimit;
  final String comment;

  BudgetSuggestion({
    required this.categoryId,
    required this.categoryName,
    required this.monthsAnalyzed,
    this.averageSpend,
    this.suggestedLimit,
    required this.comment,
  });

  factory BudgetSuggestion.fromJson(Map<String, dynamic> json) {
    return BudgetSuggestion(
      categoryId: json['categoryId'] as int? ?? 0,
      categoryName: json['categoryName'] as String? ?? '',
      monthsAnalyzed: json['monthsAnalyzed'] as int? ?? 0,
      averageSpend: (json['averageSpend'] as num?)?.toDouble(),
      suggestedLimit: (json['suggestedLimit'] as num?)?.toDouble(),
      comment: json['comment'] as String? ?? '',
    );
  }
}
