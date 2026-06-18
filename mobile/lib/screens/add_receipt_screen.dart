import 'package:flutter/material.dart';

class AddReceiptScreen extends StatefulWidget {
  const AddReceiptScreen({super.key});

  @override
  State<AddReceiptScreen> createState() => _AddReceiptScreenState();
}

class _AddReceiptScreenState extends State<AddReceiptScreen> {
  final TextEditingController _storeController = TextEditingController();
  final TextEditingController _amountController = TextEditingController();

  DateTime? _selectedDate;
  String? _selectedCategory;

  // Kategori seçenekleri (Hafta 2'de backend'den gelecek)
  final List<String> _categories = [
    'Market',
    'Giyim',
    'Elektronik',
    'Restoran',
    'Ulaşım',
    'Diğer',
  ];

  // Tarih seçici aç
  Future<void> _pickDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: now,
      firstDate: DateTime(2020),
      lastDate: now,
    );
    if (picked != null) {
      setState(() => _selectedDate = picked);
    }
  }

  // Tarihi gg.aa.yyyy formatında göster
  String _formatDate(DateTime date) {
    final day = date.day.toString().padLeft(2, '0');
    final month = date.month.toString().padLeft(2, '0');
    return '$day.$month.${date.year}';
  }

  void _handleSave() {
    final store = _storeController.text.trim();
    final amount = _amountController.text.trim();

    // Basit doğrulama
    if (store.isEmpty || amount.isEmpty || _selectedDate == null || _selectedCategory == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Lütfen tüm alanları doldur')),
      );
      return;
    }

    // TODO: Backend'e gönderilecek (Gün 11)
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Fiş kaydedildi (henüz backend yok)')),
    );

    // Forma girilen bilgiyi geri gönder ve önceki ekrana dön
    Navigator.pop(context);
  }

  @override
  void dispose() {
    _storeController.dispose();
    _amountController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Fiş Ekle'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Mağaza adı
            TextField(
              controller: _storeController,
              decoration: InputDecoration(
                labelText: 'Mağaza Adı',
                prefixIcon: const Icon(Icons.store_outlined),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),
            const SizedBox(height: 16),

            // Tutar
            TextField(
              controller: _amountController,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: InputDecoration(
                labelText: 'Tutar (TL)',
                prefixIcon: const Icon(Icons.payments_outlined),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),
            const SizedBox(height: 16),

            // Tarih seçici
            InkWell(
              onTap: _pickDate,
              borderRadius: BorderRadius.circular(12),
              child: InputDecorator(
                decoration: InputDecoration(
                  labelText: 'Tarih',
                  prefixIcon: const Icon(Icons.calendar_today_outlined),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
                child: Text(
                  _selectedDate == null
                      ? 'Tarih seç'
                      : _formatDate(_selectedDate!),
                  style: TextStyle(
                    fontSize: 16,
                    color: _selectedDate == null ? Colors.grey : Colors.black,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 16),

            // Kategori seçici (dropdown)
            DropdownButtonFormField<String>(
              initialValue: _selectedCategory,
              decoration: InputDecoration(
                labelText: 'Kategori',
                prefixIcon: const Icon(Icons.category_outlined),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              hint: const Text('Kategori seç'),
              items: _categories.map((category) {
                return DropdownMenuItem(
                  value: category,
                  child: Text(category),
                );
              }).toList(),
              onChanged: (value) {
                setState(() => _selectedCategory = value);
              },
            ),
            const SizedBox(height: 32),

            // Kaydet butonu
            ElevatedButton(
              onPressed: _handleSave,
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 16),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              child: const Text(
                'Kaydet',
                style: TextStyle(fontSize: 16),
              ),
            ),
          ],
        ),
      ),
    );
  }
}