import 'package:flutter/material.dart';
import '../models/restore_receipt_result.dart';
import '../models/category.dart';
import '../services/receipt_service.dart';
import '../core/theme/app_colors.dart';
import '../core/widgets/category_picker.dart';
import 'add_receipt_screen.dart';

/// AI restorasyonu düşük güven skoruyla döndüğünde, tüm alanları tek formda
/// göstermek yerine kullanıcıya tek tek soran bir doğrulama akışı. Her adımda
/// onaylanan bilgi bir sonraki adımı iyileştirmek için kullanılır — örn. mağaza
/// adı onaylandıktan sonra kategori önerisi AI'nin ham tahmini yerine o mağazadan
/// daha önce eklenen fişlerin geçmişine bakılarak yeniden hesaplanır.
class ReceiptVerificationScreen extends StatefulWidget {
  final RestoreReceiptResult aiResult;
  final String? imagePath;

  const ReceiptVerificationScreen({super.key, required this.aiResult, this.imagePath});

  @override
  State<ReceiptVerificationScreen> createState() => _ReceiptVerificationScreenState();
}

enum _Step { store, amount, date, category }

class _ReceiptVerificationScreenState extends State<ReceiptVerificationScreen> {
  _Step _step = _Step.store;
  late final TextEditingController _storeController;
  late final TextEditingController _amountController;
  DateTime? _selectedDate;

  List<Category> _categories = [];
  Category? _selectedCategory;
  bool _isLoadingCategories = true;
  bool _isLoadingSuggestion = false;

  @override
  void initState() {
    super.initState();
    _storeController = TextEditingController(text: widget.aiResult.storeName ?? '');
    _amountController = TextEditingController(
      text: widget.aiResult.totalAmount != null ? widget.aiResult.totalAmount!.toStringAsFixed(2) : '',
    );
    if (widget.aiResult.receiptDate != null) {
      _selectedDate = DateTime.tryParse(widget.aiResult.receiptDate!);
    }
    _loadCategories();
  }

  @override
  void dispose() {
    _storeController.dispose();
    _amountController.dispose();
    super.dispose();
  }

  Future<void> _loadCategories() async {
    try {
      final categories = await ReceiptService.getCategories();
      setState(() {
        _categories = categories;
        _isLoadingCategories = false;
      });
    } catch (_) {
      setState(() => _isLoadingCategories = false);
    }
  }

  int get _stepIndex => _Step.values.indexOf(_step);

  void _goToStep(_Step step) {
    setState(() => _step = step);
    if (step == _Step.category) _fetchCategorySuggestion();
  }

  Future<void> _fetchCategorySuggestion() async {
    if (_categories.isEmpty) return;
    setState(() => _isLoadingSuggestion = true);
    try {
      final suggestion = await ReceiptService.getCategorySuggestion(_storeController.text);
      Category? matched;
      if (suggestion != null) {
        for (final c in _categories) {
          if (c.id == suggestion.categoryId) {
            matched = c;
            break;
          }
        }
      }
      matched ??= _matchByName(widget.aiResult.suggestedCategoryName);
      if (mounted) setState(() => _selectedCategory = matched);
    } catch (_) {
      final matched = _matchByName(widget.aiResult.suggestedCategoryName);
      if (mounted) setState(() => _selectedCategory = matched);
    } finally {
      if (mounted) setState(() => _isLoadingSuggestion = false);
    }
  }

  Category? _matchByName(String? name) {
    if (name == null) return null;
    for (final c in _categories) {
      if (c.name.toLowerCase() == name.toLowerCase()) return c;
    }
    return null;
  }

  void _finish() {
    final amount = double.tryParse(_amountController.text.trim().replaceAll(',', '.'));
    final dateStr = _selectedDate != null
        ? '${_selectedDate!.year}-${_selectedDate!.month.toString().padLeft(2, '0')}-${_selectedDate!.day.toString().padLeft(2, '0')}'
        : null;

    Navigator.pushReplacement(
      context,
      MaterialPageRoute(
        builder: (context) => AddReceiptScreen(
          initialStoreName: _storeController.text.trim(),
          initialAmount: amount?.toStringAsFixed(2),
          initialDate: dateStr,
          initialImagePath: widget.imagePath,
          initialCategoryId: _selectedCategory?.id,
          initialItems: widget.aiResult.items,
        ),
      ),
    );
  }

  Widget _buildStepBody() {
    switch (_step) {
      case _Step.store:
        return _buildFieldStep(
          title: 'Mağaza adı doğru mu?',
          subtitle: widget.aiResult.storeName == null
              ? 'AI mağaza adını bulamadı — elle gir'
              : 'AI\'nin okuduğu değer aşağıda, gerekirse düzelt',
          child: TextField(
            controller: _storeController,
            autofocus: true,
            decoration: InputDecoration(
              labelText: 'Mağaza Adı',
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
            ),
          ),
          onNext: () => _goToStep(_Step.amount),
        );
      case _Step.amount:
        return _buildFieldStep(
          title: 'Tutar doğru mu?',
          subtitle: widget.aiResult.totalAmount == null
              ? 'AI tutarı bulamadı — elle gir'
              : 'AI\'nin okuduğu değer aşağıda, gerekirse düzelt',
          child: TextField(
            controller: _amountController,
            autofocus: true,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: InputDecoration(
              labelText: 'Tutar (TL)',
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
            ),
          ),
          onNext: () => _goToStep(_Step.date),
        );
      case _Step.date:
        return _buildFieldStep(
          title: 'Tarih doğru mu?',
          subtitle: widget.aiResult.receiptDate == null
              ? 'AI tarihi bulamadı — elle seç'
              : 'AI\'nin okuduğu değer aşağıda, gerekirse düzelt',
          child: InkWell(
            onTap: () async {
              final picked = await showDatePicker(
                context: context,
                initialDate: _selectedDate ?? DateTime.now(),
                firstDate: DateTime(2020),
                lastDate: DateTime.now(),
              );
              if (picked != null) setState(() => _selectedDate = picked);
            },
            borderRadius: BorderRadius.circular(12),
            child: InputDecorator(
              decoration: InputDecoration(
                labelText: 'Tarih',
                prefixIcon: const Icon(Icons.calendar_today_outlined),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
              ),
              child: Text(_selectedDate == null
                  ? 'Tarih seç'
                  : '${_selectedDate!.day}.${_selectedDate!.month}.${_selectedDate!.year}'),
            ),
          ),
          onNext: () => _goToStep(_Step.category),
        );
      case _Step.category:
        return _buildFieldStep(
          title: 'Kategori uygun mu?',
          subtitle: '"${_storeController.text}" mağazasından daha önceki fişlerine bakarak öneriyorum',
          child: CategoryPicker(
            categories: _categories,
            value: _selectedCategory,
            isLoading: _isLoadingCategories || _isLoadingSuggestion,
            onChanged: (value) => setState(() => _selectedCategory = value),
          ),
          onNext: _finish,
          isLast: true,
        );
    }
  }

  Widget _buildFieldStep({
    required String title,
    required String subtitle,
    required Widget child,
    required VoidCallback onNext,
    bool isLast = false,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(title, style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800, color: AppColors.txt(context))),
        const SizedBox(height: 6),
        Text(subtitle, style: TextStyle(fontSize: 13, color: AppColors.textSecondary, height: 1.4)),
        const SizedBox(height: 24),
        child,
        const SizedBox(height: 28),
        ElevatedButton(
          onPressed: onNext,
          style: ElevatedButton.styleFrom(
            backgroundColor: AppColors.primary,
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(vertical: 16),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          ),
          child: Text(isLast ? 'Tamamla ve Forma Aktar' : 'Onayla, Devam Et'),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Bilgileri Doğrula'),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(4),
          child: LinearProgressIndicator(
            value: (_stepIndex + 1) / _Step.values.length,
            backgroundColor: AppColors.brd(context),
            valueColor: const AlwaysStoppedAnimation<Color>(AppColors.primary),
          ),
        ),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: _buildStepBody(),
        ),
      ),
    );
  }
}
