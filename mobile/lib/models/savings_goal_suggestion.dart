class SavingsGoalSuggestion {
  final double? requiredMonthlyContribution;
  final String comment;

  SavingsGoalSuggestion({this.requiredMonthlyContribution, required this.comment});

  factory SavingsGoalSuggestion.fromJson(Map<String, dynamic> json) {
    return SavingsGoalSuggestion(
      requiredMonthlyContribution: (json['requiredMonthlyContribution'] as num?)?.toDouble(),
      comment: json['comment'] as String? ?? '',
    );
  }
}
