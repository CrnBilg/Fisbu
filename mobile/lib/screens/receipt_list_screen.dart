import 'package:flutter/material.dart';
import 'add_receipt_screen.dart';
// Geçici fiş modeli (Hafta 2'de backend'e bağlanacak)
class Receipt {
  final String storeName;
  final double totalAmount;
  final String date;
  final String category;

  Receipt({
    required this.storeName,
    required this.totalAmount,
    required this.date,
    required this.category,
  });
}

class ReceiptListScreen extends StatelessWidget {
  const ReceiptListScreen({super.key});

  // Şimdilik örnek veri. Boş listeyi test etmek için [] yapabilirsin.
List<Receipt> get _receipts => [
        Receipt(
          storeName: 'Migros',
          totalAmount: 247.50,
          date: '15.06.2026',
          category: 'Market',
        ),
        Receipt(
          storeName: 'Teknosa',
          totalAmount: 1299.00,
          date: '12.06.2026',
          category: 'Elektronik',
        ),
        Receipt(
          storeName: 'LC Waikiki',
          totalAmount: 459.90,
          date: '08.06.2026',
          category: 'Giyim',
        ),
      ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Fişlerim'),
      ),
      body: _receipts.isEmpty
          ? _buildEmptyState()
          : _buildReceiptList(),
          floatingActionButton: FloatingActionButton(
        onPressed: () {
          Navigator.push(
            context,
            MaterialPageRoute(
              builder: (context) => const AddReceiptScreen(),
            ),
          );
        },
        child: const Icon(Icons.add),
      ),
   
    );
  }

  // BOŞ DURUM — henüz fiş yokken
  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.receipt_long_outlined,
            size: 80,
            color: Colors.grey.shade300,
          ),
          const SizedBox(height: 16),
          Text(
            'Henüz fiş yok',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: Colors.grey.shade600,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'İlk fişini eklemek için + butonuna bas',
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey.shade400,
            ),
          ),
        ],
      ),
    );
  }

  // DOLU DURUM — fiş kartlarının listesi
  Widget _buildReceiptList() {
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: _receipts.length,
      itemBuilder: (context, index) {
        final receipt = _receipts[index];
        return Card(
          margin: const EdgeInsets.symmetric(vertical: 6),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          child: ListTile(
            contentPadding: const EdgeInsets.symmetric(
              horizontal: 16,
              vertical: 8,
            ),
            leading: CircleAvatar(
              backgroundColor: Colors.deepPurple.shade50,
              child: const Icon(
                Icons.receipt,
                color: Colors.deepPurple,
              ),
            ),
            title: Text(
              receipt.storeName,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
            subtitle: Text('${receipt.category} • ${receipt.date}'),
            trailing: Text(
              '${receipt.totalAmount.toStringAsFixed(2)} TL',
              style: const TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 15,
                color: Colors.deepPurple,
              ),
            ),
            onTap: () {
              // TODO: Fiş detay ekranına yönlendirilecek (Gün 10)
            },
          ),
        );
      },
    );
  }
}