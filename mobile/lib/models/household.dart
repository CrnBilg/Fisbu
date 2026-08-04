class HouseholdMember {
  final int userId;
  final String? name;
  final String email;

  HouseholdMember({required this.userId, this.name, required this.email});

  factory HouseholdMember.fromJson(Map<String, dynamic> json) {
    return HouseholdMember(
      userId: json['userId'] as int,
      name: json['name'] as String?,
      email: json['email'] as String? ?? '',
    );
  }
}

class Household {
  final int id;
  final String name;
  final String inviteCode;
  final List<HouseholdMember> members;

  Household({
    required this.id,
    required this.name,
    required this.inviteCode,
    required this.members,
  });

  factory Household.fromJson(Map<String, dynamic> json) {
    return Household(
      id: json['id'] as int,
      name: json['name'] as String? ?? '',
      inviteCode: json['inviteCode'] as String? ?? '',
      members: (json['members'] as List<dynamic>? ?? [])
          .map((e) => HouseholdMember.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}
