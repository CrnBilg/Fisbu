class ReceiptItem {
  final int? id;
  final String productName;
  final double unitPrice;
  final double quantity;

  ReceiptItem({
    this.id,
    required this.productName,
    required this.unitPrice,
    this.quantity = 1,
  });

  factory ReceiptItem.fromJson(Map<String, dynamic> json) {
    return ReceiptItem(
      id: json['id'] as int?,
      productName: json['productName'] as String? ?? '',
      unitPrice: (json['unitPrice'] as num).toDouble(),
      quantity: (json['quantity'] as num?)?.toDouble() ?? 1,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'productName': productName,
      'unitPrice': unitPrice,
      'quantity': quantity,
    };
  }
}
