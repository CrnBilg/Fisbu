import 'package:flutter/material.dart';
import 'add_receipt_screen.dart';
import 'receipt_detail_screen.dart';
import '../models/receipt.dart';
import '../services/receipt_service.dart';

class ReceiptListScreen extends StatefulWidget {
  const ReceiptListScreen({super.key});

  @override
  State<ReceiptListScreen> createState() => _ReceiptListScreenState();
}

class _ReceiptListScreenState extends State<ReceiptListScreen> {
  List<Receipt> _receipts = [];
  bool _isLoading = true;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadReceipts();
  }

  // Fişleri backend'den çeker
  Future<void> _loadReceipts() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final receipts = await ReceiptService.getReceipts();
      setState(() {
        _receipts = receipts;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _errorMessage = 'Fişler yüklenemedi: $e';
        _isLoading = false;
      });
    }
  }

  // Fiş ekleme ekranına git, döndüğünde listeyi yenile
  Future<void> _goToAddReceipt() async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => const AddReceiptScreen(),
      ),
    );

    // AddReceiptScreen "true" döndürdüyse (yeni fiş eklendiyse) listeyi yenile
    if (result == true) {
      _loadReceipts();
    }
  }

  // Fiş detayına git, silindiyse listeyi yenile
  Future<void> _goToDetail(Receipt receipt) async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => ReceiptDetailScreen(receipt: receipt),
      ),
    );

    if (result == true) {
      _loadReceipts();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Fişlerim'),
      ),
      body: _buildBody(),
      floatingActionButton: FloatingActionButton(
        onPressed: _goToAddReceipt,
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null) {
      return _buildErrorState();
    }

    if (_receipts.isEmpty) {
      return _buildEmptyState();
    }

    return _buildReceiptList();
  }

  // HATA DURUMU — backend'e ulaşılamadığında
  Widget _buildErrorState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.error_outline,
            size: 80,
            color: Colors.red.shade200,
          ),
          const SizedBox(height: 16),
          Text(
            _errorMessage!,
            textAlign: TextAlign.center,
            style: TextStyle(color: Colors.grey.shade600),
          ),
          const SizedBox(height: 16),
          ElevatedButton(
            onPressed: _loadReceipts,
            child: const Text('Tekrar Dene'),
          ),
        ],
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
    return RefreshIndicator(
      onRefresh: _loadReceipts,
      child: ListView.builder(
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
              subtitle: Text(
                '${receipt.categoryName ?? "Kategorisiz"} • ${receipt.receiptDate}',
              ),
              trailing: Text(
                '${receipt.totalAmount.toStringAsFixed(2)} TL',
                style: const TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 15,
                  color: Colors.deepPurple,
                ),
              ),
              onTap: () => _goToDetail(receipt),
            ),
          );
        },
      ),
    );
  }
}