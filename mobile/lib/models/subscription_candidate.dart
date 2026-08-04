class SubscriptionCandidate {
  final String storeName;
  final double averageAmount;
  final int occurrenceCount;
  final String firstDate;
  final String lastDate;
  final int averageIntervalDays;
  final String estimatedNextDate;

  SubscriptionCandidate({
    required this.storeName,
    required this.averageAmount,
    required this.occurrenceCount,
    required this.firstDate,
    required this.lastDate,
    required this.averageIntervalDays,
    required this.estimatedNextDate,
  });

  factory SubscriptionCandidate.fromJson(Map<String, dynamic> json) {
    return SubscriptionCandidate(
      storeName: json['storeName'] as String? ?? 'Diğer',
      averageAmount: (json['averageAmount'] as num?)?.toDouble() ?? 0,
      occurrenceCount: json['occurrenceCount'] as int? ?? 0,
      firstDate: json['firstDate'] as String? ?? '',
      lastDate: json['lastDate'] as String? ?? '',
      averageIntervalDays: json['averageIntervalDays'] as int? ?? 0,
      estimatedNextDate: json['estimatedNextDate'] as String? ?? '',
    );
  }
}
