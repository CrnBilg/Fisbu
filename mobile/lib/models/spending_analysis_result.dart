class SpendingAnalysisResult {
  final int year;
  final int month;
  final double totalAmount;
  final String comment;

  SpendingAnalysisResult({
    required this.year,
    required this.month,
    required this.totalAmount,
    required this.comment,
  });

  factory SpendingAnalysisResult.fromJson(Map<String, dynamic> json) {
    return SpendingAnalysisResult(
      year: json['year'] as int,
      month: json['month'] as int,
      totalAmount: (json['totalAmount'] as num?)?.toDouble() ?? 0,
      comment: json['comment'] as String? ?? '',
    );
  }
}
