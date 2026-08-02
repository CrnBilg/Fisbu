class SplitParticipant {
  final String name;
  final double amount;

  SplitParticipant({required this.name, required this.amount});

  factory SplitParticipant.fromJson(Map<String, dynamic> json) {
    return SplitParticipant(
      name: json['name'] as String? ?? '',
      amount: (json['amount'] as num).toDouble(),
    );
  }

  Map<String, dynamic> toJson() => {'name': name, 'amount': amount};
}
