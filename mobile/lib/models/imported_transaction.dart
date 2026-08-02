class ImportedTransaction {
  String? date;
  String description;
  double amount;
  String? suggestedCategoryName;
  int? matchedCategoryId;
  final int confidenceScore;
  bool selected;

  ImportedTransaction({
    this.date,
    required this.description,
    required this.amount,
    this.suggestedCategoryName,
    this.matchedCategoryId,
    required this.confidenceScore,
    this.selected = true,
  });

  factory ImportedTransaction.fromJson(Map<String, dynamic> json) {
    return ImportedTransaction(
      date: json['date'] as String?,
      description: (json['description'] as String?) ?? '',
      amount: (json['amount'] as num?)?.toDouble() ?? 0,
      suggestedCategoryName: json['suggestedCategoryName'] as String?,
      matchedCategoryId: json['matchedCategoryId'] as int?,
      confidenceScore: (json['confidenceScore'] as num?)?.toInt() ?? 0,
    );
  }
}
