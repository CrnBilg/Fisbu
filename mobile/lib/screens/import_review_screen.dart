import 'package:flutter/material.dart';
import '../services/receipt_service.dart';
import '../models/category.dart';
import '../models/imported_transaction.dart';
import '../models/parsed_statement_result.dart';
import '../core/theme/app_colors.dart';
import '../core/widgets/category_picker.dart';

class _RowState {
  final ImportedTransaction original;
  final TextEditingController descController;
  final TextEditingController amountController;
  DateTime? date;
  Category? category;
  bool selected;

  _RowState(this.original)
      : descController = TextEditingController(text: original.description),
        amountController = TextEditingController(text: original.amount.toStringAsFixed(2)),
        date = original.date != null ? DateTime.tryParse(original.date!) : null,
        selected = true;

  void dispose() {
    descController.dispose();
    amountController.dispose();
  }
}

class ImportReviewScreen extends StatefulWidget {
  final ParsedStatementResult result;

  const ImportReviewScreen({super.key, required this.result});

  @override
  State<ImportReviewScreen> createState() => _ImportReviewScreenState();
}

class _ImportReviewScreenState extends State<ImportReviewScreen> {
  late final List<_RowState> _rows;
  List<Category> _categories = [];
  bool _isCategoriesLoading = true;
  bool _isSubmitting = false;

  bool get _isAiExtracted => widget.result.sourceType == 'AI_EXTRACTED';

  @override
  void initState() {
    super.initState();
    _rows = widget.result.transactions.map((t) => _RowState(t)).toList();
    _loadCategories();
  }

  Future<void> _loadCategories() async {
    try {
      final categories = await ReceiptService.getCategories();
      setState(() {
        _categories = categories;
        _isCategoriesLoading = false;
        for (final row in _rows) {
          final matchedId = row.original.matchedCategoryId;
          if (matchedId != null) {
            for (final category in categories) {
              if (category.id == matchedId) {
                row.category = category;
                break;
              }
            }
          }
        }
      });
    } catch (e) {
      setState(() => _isCategoriesLoading = false);
    }
  }

  @override
  void dispose() {
    for (final row in _rows) {
      row.dispose();
    }
    super.dispose();
  }

  Future<void> _pickDate(_RowState row) async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: row.date ?? now,
      firstDate: DateTime(2020),
      lastDate: now,
    );
    if (picked != null) {
      setState(() => row.date = picked);
    }
  }

  String _formatDate(DateTime date) {
    return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
  }

  String _formatDateDisplay(DateTime date) {
    return '${date.day.toString().padLeft(2, '0')}.${date.month.toString().padLeft(2, '0')}.${date.year}';
  }

  Color _confidenceColor(int score) {
    if (score >= 90) return AppColors.success;
    if (score >= 60) return AppColors.warning;
    return AppColors.error;
  }

  int get _selectedCount => _rows.where((r) => r.selected).length;

  Future<void> _confirm() async {
    final selectedRows = _rows.where((r) => r.selected).toList();
    if (selectedRows.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Lütfen en az bir işlem seç')),
      );
      return;
    }

    final transactions = <ImportedTransaction>[];
    for (final row in selectedRows) {
      final desc = row.descController.text.trim();
      final amount = double.tryParse(row.amountController.text.trim().replaceAll(',', '.'));
      if (desc.isEmpty || amount == null || amount <= 0 || row.date == null) {
        continue;
      }
      transactions.add(ImportedTransaction(
        date: _formatDate(row.date!),
        description: desc,
        amount: amount,
        matchedCategoryId: row.category?.id,
        confidenceScore: row.original.confidenceScore,
      ));
    }

    if (transactions.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Seçilen işlemlerde eksik/geçersiz alanlar var (mağaza, tutar, tarih)')),
      );
      return;
    }

    setState(() => _isSubmitting = true);

    try {
      final result = await ReceiptService.confirmImport(transactions);
      if (!mounted) return;
      final failedCount = result.failed.length;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(
          failedCount == 0
              ? '${result.createdCount} fiş başarıyla eklendi'
              : '${result.createdCount} fiş eklendi, $failedCount başarısız oldu',
        )),
      );
      Navigator.pop(context);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Hata: $e')));
      }
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('İşlemleri Gözden Geçir (${_rows.length})')),
      body: Column(
        children: [
          if (widget.result.warnings.isNotEmpty)
            Container(
              width: double.infinity,
              margin: const EdgeInsets.fromLTRB(20, 12, 20, 0),
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppColors.errDim(context),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.warning.withOpacity(0.3)),
              ),
              child: Text(
                widget.result.warnings.join('\n'),
                style: const TextStyle(color: AppColors.warning, fontSize: 12),
              ),
            ),
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.all(20),
              itemCount: _rows.length,
              itemBuilder: (context, index) {
                final row = _rows[index];
                return Container(
                  margin: const EdgeInsets.only(bottom: 14),
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: AppColors.surf(context),
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(
                      color: row.selected ? AppColors.primary.withOpacity(0.4) : AppColors.brd(context),
                      width: row.selected ? 1.5 : 1,
                    ),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Row(
                        children: [
                          Checkbox(
                            value: row.selected,
                            onChanged: (v) => setState(() => row.selected = v ?? false),
                            activeColor: AppColors.primary,
                          ),
                          if (_isAiExtracted)
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                              decoration: BoxDecoration(
                                color: _confidenceColor(row.original.confidenceScore).withOpacity(0.15),
                                borderRadius: BorderRadius.circular(20),
                              ),
                              child: Text(
                                '%${row.original.confidenceScore}',
                                style: TextStyle(
                                  fontSize: 11,
                                  fontWeight: FontWeight.w700,
                                  color: _confidenceColor(row.original.confidenceScore),
                                ),
                              ),
                            ),
                        ],
                      ),
                      TextField(
                        controller: row.descController,
                        decoration: InputDecoration(
                          labelText: 'Mağaza / Açıklama',
                          isDense: true,
                          border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
                          filled: true,
                          fillColor: AppColors.bg(context),
                        ),
                      ),
                      const SizedBox(height: 10),
                      Row(
                        children: [
                          Expanded(
                            child: TextField(
                              controller: row.amountController,
                              keyboardType: const TextInputType.numberWithOptions(decimal: true),
                              decoration: InputDecoration(
                                labelText: 'Tutar (TL)',
                                isDense: true,
                                border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
                                filled: true,
                                fillColor: AppColors.bg(context),
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: InkWell(
                              onTap: () => _pickDate(row),
                              borderRadius: BorderRadius.circular(10),
                              child: InputDecorator(
                                decoration: InputDecoration(
                                  labelText: 'Tarih',
                                  isDense: true,
                                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
                                  filled: true,
                                  fillColor: AppColors.bg(context),
                                ),
                                child: Text(
                                  row.date == null ? 'Seç' : _formatDateDisplay(row.date!),
                                  style: TextStyle(
                                    fontSize: 14,
                                    color: row.date == null ? AppColors.textTertiary : AppColors.txt(context),
                                  ),
                                ),
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 10),
                      CategoryPicker(
                        categories: _categories,
                        value: row.category,
                        isLoading: _isCategoriesLoading,
                        onChanged: (value) => setState(() => row.category = value),
                      ),
                    ],
                  ),
                );
              },
            ),
          ),
          SafeArea(
            top: false,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 8, 20, 16),
              child: ElevatedButton(
                onPressed: _isSubmitting ? null : _confirm,
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.primary,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
                child: _isSubmitting
                    ? const SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                      )
                    : Text('Seçilenleri İçe Aktar ($_selectedCount)', style: const TextStyle(fontSize: 16)),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
