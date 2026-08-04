class SpendingPersona {
  final String title;
  final String description;

  SpendingPersona({required this.title, required this.description});

  factory SpendingPersona.fromJson(Map<String, dynamic> json) {
    return SpendingPersona(
      title: json['title'] as String? ?? '',
      description: json['description'] as String? ?? '',
    );
  }
}

class SpendingBadge {
  final String id;
  final String title;
  final String description;
  final bool achieved;

  SpendingBadge({required this.id, required this.title, required this.description, required this.achieved});

  factory SpendingBadge.fromJson(Map<String, dynamic> json) {
    return SpendingBadge(
      id: json['id'] as String? ?? '',
      title: json['title'] as String? ?? '',
      description: json['description'] as String? ?? '',
      achieved: json['achieved'] as bool? ?? false,
    );
  }
}

class SpendingPersonality {
  final SpendingPersona persona;
  final List<SpendingBadge> badges;

  SpendingPersonality({required this.persona, required this.badges});

  factory SpendingPersonality.fromJson(Map<String, dynamic> json) {
    return SpendingPersonality(
      persona: SpendingPersona.fromJson(json['persona'] as Map<String, dynamic>),
      badges: (json['badges'] as List<dynamic>? ?? [])
          .map((e) => SpendingBadge.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}
