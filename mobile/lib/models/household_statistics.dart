class HouseholdMemberTotal {
  final int userId;
  final String? name;
  final String email;
  final double totalAmount;

  HouseholdMemberTotal({
    required this.userId,
    this.name,
    required this.email,
    required this.totalAmount,
  });

  factory HouseholdMemberTotal.fromJson(Map<String, dynamic> json) {
    return HouseholdMemberTotal(
      userId: json['userId'] as int,
      name: json['name'] as String?,
      email: json['email'] as String? ?? '',
      totalAmount: (json['totalAmount'] as num?)?.toDouble() ?? 0,
    );
  }
}

class HouseholdCategoryTotal {
  final String categoryName;
  final double totalAmount;

  HouseholdCategoryTotal({required this.categoryName, required this.totalAmount});

  factory HouseholdCategoryTotal.fromJson(Map<String, dynamic> json) {
    return HouseholdCategoryTotal(
      categoryName: json['categoryName'] as String? ?? 'Diğer',
      totalAmount: (json['totalAmount'] as num?)?.toDouble() ?? 0,
    );
  }
}

class HouseholdStatistics {
  final int year;
  final int month;
  final double totalAmount;
  final List<HouseholdMemberTotal> byMember;
  final List<HouseholdCategoryTotal> byCategory;

  HouseholdStatistics({
    required this.year,
    required this.month,
    required this.totalAmount,
    required this.byMember,
    required this.byCategory,
  });

  factory HouseholdStatistics.fromJson(Map<String, dynamic> json) {
    return HouseholdStatistics(
      year: json['year'] as int? ?? DateTime.now().year,
      month: json['month'] as int? ?? DateTime.now().month,
      totalAmount: (json['totalAmount'] as num?)?.toDouble() ?? 0,
      byMember: (json['byMember'] as List<dynamic>? ?? [])
          .map((e) => HouseholdMemberTotal.fromJson(e as Map<String, dynamic>))
          .toList(),
      byCategory: (json['byCategory'] as List<dynamic>? ?? [])
          .map((e) => HouseholdCategoryTotal.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}
