class SavingsGoal {
  final int id;
  final String name;
  final double targetAmount;
  final double currentAmount;
  final String? targetDate;
  final double progressPercent;
  final bool achieved;

  SavingsGoal({
    required this.id,
    required this.name,
    required this.targetAmount,
    required this.currentAmount,
    this.targetDate,
    required this.progressPercent,
    required this.achieved,
  });

  factory SavingsGoal.fromJson(Map<String, dynamic> json) {
    return SavingsGoal(
      id: json['id'] as int,
      name: json['name'] as String? ?? '',
      targetAmount: (json['targetAmount'] as num?)?.toDouble() ?? 0,
      currentAmount: (json['currentAmount'] as num?)?.toDouble() ?? 0,
      targetDate: json['targetDate'] as String?,
      progressPercent: (json['progressPercent'] as num?)?.toDouble() ?? 0,
      achieved: json['achieved'] as bool? ?? false,
    );
  }
}
