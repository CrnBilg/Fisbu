import 'package:flutter/material.dart';
import '../models/receipt.dart';
import '../services/receipt_service.dart';

class ReceiptDetailScreen extends StatefulWidget {
  final Receipt receipt;

  const ReceiptDetailScreen({super.key, required this.receipt});

  @override
  State<ReceiptDetailScreen> createState() => _ReceiptDetailScreenState();
}

class _ReceiptDetailScreenState extends State<ReceiptDetailScreen> {
  bool _isDeleting = false;

  Future<void> _confirmDelete() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Fişi Sil'),
        content: const Text('Bu fişi silmek istediğine emin misin?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Vazgeç'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Sil', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      _deleteReceipt();
    }
  }

  Future<void> _deleteReceipt() async {
    setState(() => _isDeleting = true);

    try {
      await ReceiptService.deleteReceipt(widget.receipt.id);

      if (!mounted) return;

      // Detay ekranını kapat, liste ekranına "true" gönder (listeyi yenile)
      Navigator.pop(context, true);
    } catch (e) {
      setState(() => _isDeleting = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Fiş silinemedi: $e')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final receipt = widget.receipt;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Fiş Detayı'),
        actions: [
          IconButton(
            icon: _isDeleting
                ? const SizedBox(
                    height: 20,
                    width: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.delete_outline),
            onPressed: _isDeleting ? null : _confirmDelete,
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
              value: receipt.categoryName ?? 'Kategorisiz',
            ),
            const Divider(),
            _buildDetailRow(
              icon: Icons.calendar_today_outlined,
              label: 'Tarih',
              value: receipt.receiptDate,
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