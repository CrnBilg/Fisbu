class Budget {
  final int id;
  final int categoryId;
  final String categoryName;
  final String? categoryColor;
  final double monthlyLimit;
  final int year;
  final int month;
  final double currentSpend;
  final double percentage;
  final bool overBudget;

  Budget({
    required this.id,
    required this.categoryId,
    required this.categoryName,
    this.categoryColor,
    required this.monthlyLimit,
    required this.year,
    required this.month,
    required this.currentSpend,
    required this.percentage,
    required this.overBudget,
  });

  factory Budget.fromJson(Map<String, dynamic> json) {
    return Budget(
      id: json['id'] as int,
      categoryId: json['categoryId'] as int,
      categoryName: json['categoryName'] as String? ?? 'Diğer',
      categoryColor: json['categoryColor'] as String?,
      monthlyLimit: (json['monthlyLimit'] as num).toDouble(),
      year: json['year'] as int,
      month: json['month'] as int,
      currentSpend: (json['currentSpend'] as num?)?.toDouble() ?? 0,
      percentage: (json['percentage'] as num?)?.toDouble() ?? 0,
      overBudget: json['overBudget'] as bool? ?? false,
    );
  }
}
