class Category {// Kategori modeli
  final int id;
  final String name;
  final String? color;

  Category({ // Kategori oluşturucu
    required this.id,
    required this.name,
    this.color,
  });

  factory Category.fromJson(Map<String, dynamic> json) { // JSON'dan kategori oluşturuyor
    return Category(
      id: json['id'] as int,// ID'yi aliyoruz
      name: json['name'] as String, // İsmi aliyoruz
      color: json['color'] as String?, // Rengi aliyoruz 
    );
  }
}