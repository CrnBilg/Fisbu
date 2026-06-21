class Receipt { // Fiş modeli
  final int id;
  final String storeName;
  final double totalAmount;
  final String receiptDate;
  final int? categoryId;
  final String? categoryName;

  Receipt({ // Fiş oluşturucu
    required this.id,
    required this.storeName,
    required this.totalAmount,
    required this.receiptDate,
    this.categoryId,
    this.categoryName,
  });

  factory Receipt.fromJson(Map<String, dynamic> json) { // JSON'dan fiş oluşturuyoruz
    return Receipt(
      id: json['id'] as int, // ID'yi aliyoruz
      storeName: json['storeName'] as String? ?? '', // Mağaza adini aliyoruz, null ise boş string olacak
      totalAmount: (json['totalAmount'] as num).toDouble(), // Toplam tutari aliyoruz, num'dan double'a çeviriyoruz
      receiptDate: json['receiptDate'] as String? ?? '',// Fiş tarihini aliyoruz, null ise boş string olacak
      categoryId: json['categoryId'] as int?, // Kategori ID'sini aliyoruz, null olabilir
      categoryName: json['categoryName'] as String?,// Kategori adini aliyoruz, null olabilir
    );
  }
}