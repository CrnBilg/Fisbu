import 'package:flutter/material.dart';
import 'receipt_list_screen.dart'; // Receipt modeli buradan geliyor

class ReceiptDetailScreen extends StatelessWidget {
  final Receipt receipt;

  const ReceiptDetailScreen({super.key, required this.receipt});

  void _confirmDelete(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Fişi Sil'),
        content: const Text('Bu fişi silmek istediğine emin misin?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Vazgeç'),
          ),
          TextButton(
            onPressed: () {
              // TODO: Backend'e silme isteği gönderilecek (Gün 12)
              Navigator.pop(context); // dialog'u kapat
              Navigator.pop(context); // detay ekranını kapat
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Fiş silindi (henüz backend yok)')),
              );
            },
            child: const Text('Sil', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Fiş Detayı'),
        actions: [
          IconButton(
            icon: const Icon(Icons.delete_outline),
            onPressed: () => _confirmDelete(context),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Üst kısım: mağaza adı ve tutar
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: Colors.deepPurple.shade50,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Column(
                children: [
                  CircleAvatar(
                    radius: 32,
                    backgroundColor: Colors.deepPurple.shade100,
                    child: const Icon(
                      Icons.receipt_long,
                      size: 32,
                      color: Colors.deepPurple,
                    ),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    receipt.storeName,
                    style: const TextStyle(
                      fontSize: 22,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '${receipt.totalAmount.toStringAsFixed(2)} TL',
                    style: const TextStyle(
                      fontSize: 28,
                      fontWeight: FontWeight.bold,
                      color: Colors.deepPurple,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),

            // Detay satırları
            _buildDetailRow(
              icon: Icons.category_outlined,
              label: 'Kategori',
              value: receipt.category,
            ),
            const Divider(),
            _buildDetailRow(
              icon: Icons.calendar_today_outlined,
              label: 'Tarih',
              value: receipt.date,
            ),
            const Divider(),
            _buildDetailRow(
              icon: Icons.store_outlined,
              label: 'Mağaza',
              value: receipt.storeName,
            ),
          ],
        ),
      ),
    );
  }

  // Tek bir detay satırı (ikon + etiket + değer)
  Widget _buildDetailRow({
    required IconData icon,
    required String label,
    required String value,
  }) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Row(
        children: [
          Icon(icon, color: Colors.deepPurple),
          const SizedBox(width: 16),
          Text(
            label,
            style: const TextStyle(
              fontSize: 15,
              color: Colors.grey,
            ),
          ),
          const Spacer(),
          Text(
            value,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }
}