import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../models/category.dart';
import '../models/budget.dart';
import '../services/receipt_service.dart';
import '../services/budget_service.dart';
import '../core/theme/app_colors.dart';

class BudgetScreen extends StatefulWidget {
  const BudgetScreen({super.key});

  @override
  State<BudgetScreen> createState() => _BudgetScreenState();
}

class _BudgetScreenState extends State<BudgetScreen> {
  List<Category> _categories = [];
  List<Budget> _budgets = [];
  bool _isLoading = true;
  String? _errorMessage;
  final _currencyFormat = NumberFormat('#,##0.00', 'tr_TR');

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final results = await Future.wait([
        ReceiptService.getCategories(),
        BudgetService.getBudgets(),
      ]);
      setState(() {
        _categories = results[0] as List<Category>;
        _budgets = results[1] as List<Budget>;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _errorMessage = 'Bütçeler yüklenemedi: $e';
        _isLoading = false;
      });
    }
  }

  Budget? _budgetForCategory(int categoryId) {
    try {
      return _budgets.firstWhere((b) => b.categoryId == categoryId);
    } catch (_) {
      return null;
    }
  }

  Color _percentageColor(double percentage) {
    if (percentage > 100) return AppColors.error;
    if (percentage >= 80) return AppColors.warning;
    return AppColors.primary;
  }

  Future<void> _showSetBudgetDialog(Category category, {Budget? existing}) async {
    final limitController = TextEditingController(
      text: existing != null ? existing.monthlyLimit.toStringAsFixed(0) : '',
    );

    final result = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Text(
          existing == null ? 'Bütçe Belirle' : 'Bütçeyi Düzenle',
          style: const TextStyle(fontWeight: FontWeight.w700),
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              category.name,
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: limitController,
              autofocus: true,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(
                labelText: 'Aylık limit (TL)',
                hintText: 'örn. 10000',
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Vazgeç'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(context, true),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
            ),
            child: const Text('Kaydet'),
          ),
        ],
      ),
    );

    if (result != true) return;

    final limit = double.tryParse(limitController.text.trim().replaceAll(',', '.'));
    if (limit == null || limit <= 0) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Geçerli bir tutar gir')),
        );
      }
      return;
    }

    final now = DateTime.now();
    try {
      if (existing == null) {
        await BudgetService.createBudget(
          categoryId: category.id,
          monthlyLimit: limit,
          year: now.year,
          month: now.month,
        );
      } else {
        await BudgetService.updateBudget(
          id: existing.id,
          categoryId: category.id,
          monthlyLimit: limit,
          year: now.year,
          month: now.month,
        );
      }
      _loadData();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('$e')),
        );
      }
    }
  }

  Future<void> _deleteBudget(Budget budget) async {
    try {
      await BudgetService.deleteBudget(budget.id);
      _loadData();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Bütçe kaldırıldı')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Silinemedi: $e')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Bütçelerim')),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator(color: AppColors.primary));
    }

    if (_errorMessage != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(_errorMessage!),
            const SizedBox(height: 16),
            ElevatedButton(onPressed: _loadData, child: const Text('Tekrar Dene')),
          ],
        ),
      );
    }

    if (_categories.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: AppColors.primDim(context),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.pie_chart_outline, size: 48, color: AppColors.primary),
            ),
            const SizedBox(height: 16),
            Text(
              'Önce bir kategori oluştur',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: AppColors.txt(context),
              ),
            ),
          ],
        ),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
      itemCount: _categories.length,
      itemBuilder: (context, index) {
        final category = _categories[index];
        final budget = _budgetForCategory(category.id);

        if (budget == null) {
          return GestureDetector(
            onTap: () => _showSetBudgetDialog(category),
            child: Container(
              margin: const EdgeInsets.only(bottom: 10),
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
              decoration: BoxDecoration(
                color: AppColors.surf(context),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: AppColors.brd(context)),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      category.name,
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                        color: AppColors.txt(context),
                      ),
                    ),
                  ),
                  Text(
                    'Bütçe belirlenmedi',
                    style: TextStyle(fontSize: 13, color: AppColors.textSecondary),
                  ),
                  const SizedBox(width: 8),
                  Icon(Icons.add_circle_outline, color: AppColors.primary, size: 20),
                ],
              ),
            ),
          );
        }

        final color = _percentageColor(budget.percentage);

        return Dismissible(
          key: Key('budget_${budget.id}'),
          direction: DismissDirection.endToStart,
          background: Container(
            margin: const EdgeInsets.only(bottom: 10),
            decoration: BoxDecoration(
              color: AppColors.error,
              borderRadius: BorderRadius.circular(16),
            ),
            alignment: Alignment.centerRight,
            padding: const EdgeInsets.only(right: 20),
            child: const Icon(Icons.delete_outline, color: Colors.white, size: 26),
          ),
          confirmDismiss: (_) async {
            final confirmed = await showDialog<bool>(
              context: context,
              builder: (context) => AlertDialog(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                title: const Text('Bütçeyi Kaldır', style: TextStyle(fontWeight: FontWeight.w700)),
                content: Text('"${category.name}" için belirlenen bütçeyi kaldırmak istediğine emin misin?'),
                actions: [
                  TextButton(
                    onPressed: () => Navigator.pop(context, false),
                    child: const Text('Vazgeç'),
                  ),
                  TextButton(
                    onPressed: () => Navigator.pop(context, true),
                    style: TextButton.styleFrom(foregroundColor: AppColors.error),
                    child: const Text('Kaldır'),
                  ),
                ],
              ),
            );
            return confirmed == true;
          },
          onDismissed: (_) => _deleteBudget(budget),
          child: GestureDetector(
            onTap: () => _showSetBudgetDialog(category, existing: budget),
            child: Container(
              margin: const EdgeInsets.only(bottom: 10),
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppColors.surf(context),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: AppColors.brd(context)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          category.name,
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w600,
                            color: AppColors.txt(context),
                          ),
                        ),
                      ),
                      Text(
                        '%${budget.percentage.toStringAsFixed(0)}',
                        style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700, color: color),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  ClipRRect(
                    borderRadius: BorderRadius.circular(4),
                    child: LinearProgressIndicator(
                      value: (budget.percentage / 100).clamp(0.0, 1.0),
                      backgroundColor: AppColors.brd(context),
                      valueColor: AlwaysStoppedAnimation<Color>(color),
                      minHeight: 6,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '${_currencyFormat.format(budget.currentSpend)} / ${_currencyFormat.format(budget.monthlyLimit)} TL',
                    style: TextStyle(fontSize: 13, color: AppColors.textSecondary),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}
