import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'profile_screen.dart';
import 'add_receipt_screen.dart';
import 'receipt_list_screen.dart';
import 'ocr_screen.dart';
import 'statistics_screen.dart';
import '../services/receipt_service.dart';
import '../models/receipt.dart';
import '../core/utils/date_formatter.dart';
import '../core/utils/category_helper.dart';
import '../core/theme/app_colors.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  List<Receipt> _receipts = [];
  bool _isLoading = true;
  final _currencyFormat = NumberFormat('#,##0.00', 'tr_TR');

  @override
  void initState() {
    super.initState();
    _loadReceipts();
  }

  Future<void> _loadReceipts() async {
    try {
      final receipts = await ReceiptService.getReceipts();
      setState(() {
        _receipts = receipts;
        _isLoading = false;
      });
    } catch (e) {
      setState(() => _isLoading = false);
    }
  }

  double get _thisMonthTotal {
    final now = DateTime.now();
    return _receipts.where((r) {
      try {
        final date = DateTime.parse(r.receiptDate);
        return date.year == now.year && date.month == now.month;
      } catch (_) {
        return false;
      }
    }).fold(0.0, (sum, r) => sum + r.totalAmount);
  }

  List<Receipt> get _recentReceipts {
    final sorted = List<Receipt>.from(_receipts)
      ..sort((a, b) => (b.createdAt ?? '').compareTo(a.createdAt ?? ''));
    return sorted.take(3).toList();
  }

  String? get _topCategory {
    if (_receipts.isEmpty) return null;
    final Map<String, double> categoryTotals = {};
    for (final receipt in _receipts) {
      final category = receipt.categoryName ?? 'Kategorisiz';
      categoryTotals[category] = (categoryTotals[category] ?? 0) + receipt.totalAmount;
    }
    return categoryTotals.entries
        .reduce((a, b) => a.value > b.value ? a : b)
        .key;
  }

  double _categoryTotal(String categoryName) {
    return _receipts
        .where((r) => (r.categoryName ?? 'Kategorisiz') == categoryName)
        .fold(0.0, (sum, r) => sum + r.totalAmount);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(

      body: RefreshIndicator(
        onRefresh: _loadReceipts,
        child: CustomScrollView(
          slivers: [
            SliverAppBar(
              expandedHeight: 200,
              floating: false,
              pinned: true,
              backgroundColor: AppColors.primary,
              foregroundColor: Colors.white,
              centerTitle: false,
              title: Text(
                'FişBu',
                style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w800,
                  fontSize: 22,
                ),
              ),
              actions: [
                IconButton(
                  icon: const CircleAvatar(
                    backgroundColor: Colors.white24,
                    child: Icon(Icons.person_outline, color: Colors.white, size: 20),
                  ),
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(builder: (context) => const ProfileScreen()),
                    );
                  },
                ),
                const SizedBox(width: 8),
              ],
              flexibleSpace: FlexibleSpaceBar(
                background: Container(
                 decoration: BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                      colors: Theme.of(context).brightness == Brightness.dark
                          ? [AppColors.primaryDimDark, AppColors.surfaceDark]
                          : [AppColors.primary, AppColors.primaryDark],
                    ),
                  ),
                  child: SafeArea(
                    child: Padding(
                      padding: const EdgeInsets.fromLTRB(20, 60, 20, 20),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          Text(
                            'Bu Ay Harcamaların',
                            style: TextStyle(
                              color: Colors.white70,
                              fontSize: 13,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                          const SizedBox(height: 4),
                          _isLoading
                              ? const SizedBox(
                                  height: 40,
                                  width: 40,
                                  child: CircularProgressIndicator(
                                    color: Colors.white,
                                    strokeWidth: 2,
                                  ),
                                )
                              : Text(
                                  '${_currencyFormat.format(_thisMonthTotal)} TL',
                                  style: TextStyle(
                                    color: Colors.white,
                                    fontSize: 36,
                                    fontWeight: FontWeight.w800,
                                    letterSpacing: -1,
                                  ),
                                ),
                          const SizedBox(height: 4),
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 10,
                              vertical: 4,
                            ),
                            decoration: BoxDecoration(
                              color: Colors.white24,
                              borderRadius: BorderRadius.circular(20),
                            ),
                            child: Text(
                              _receipts.isEmpty
                                  ? 'Henüz fiş eklenmedi'
                                  : '${_receipts.length} fiş kaydedildi',
                              style: TextStyle(
                                color: Colors.white,
                                fontSize: 12,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),

            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: _QuickActionCard(
                            icon: Icons.add_circle_outline,
                            label: 'Fiş Ekle',
                            color: AppColors.primary,
                            onTap: () async {
                              await Navigator.push(
                                context,
                                MaterialPageRoute(
                                  builder: (context) => const AddReceiptScreen(),
                                ),
                              );
                              _loadReceipts();
                            },
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: _QuickActionCard(
                            icon: Icons.receipt_long_outlined,
                            label: 'Tüm Fişler',
                            color: AppColors.secondary,
                            onTap: () async {
                              await Navigator.push(
                                context,
                                MaterialPageRoute(
                                  builder: (context) => const ReceiptListScreen(),
                                ),
                              );
                              _loadReceipts();
                            },
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    _QuickActionCard(
                      icon: Icons.bar_chart_outlined,
                      label: 'İstatistikler',
                      color: const Color(0xFFFF6B6B),
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (context) => const StatisticsScreen(),
                          ),
                        );
                      },
                    ),
                    const SizedBox(height: 12),
                    _QuickActionCard(
                      icon: Icons.document_scanner_outlined,
                      label: 'Fişi Tara',
                      color: Color(0xFF8B5CF6),
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (context) => const OcrScreen(),
                          ),
                        );
                      },
                    ),

                    if (_topCategory != null && !_isLoading)
                      Container(
                        margin: const EdgeInsets.only(bottom: 20),
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: CategoryHelper.getColor(_topCategory).withOpacity(0.08),
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(
                            color: CategoryHelper.getColor(_topCategory).withOpacity(0.2),
                          ),
                        ),
                        child: Row(
                          children: [
                            Container(
                              padding: const EdgeInsets.all(10),
                              decoration: BoxDecoration(
                                color: CategoryHelper.getColor(_topCategory).withOpacity(0.15),
                                borderRadius: BorderRadius.circular(12),
                              ),
                              child: Icon(
                                CategoryHelper.getIcon(_topCategory),
                                color: CategoryHelper.getColor(_topCategory),
                                size: 22,
                              ),
                            ),
                            const SizedBox(width: 14),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  'Bu ay en çok harcama',
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: AppColors.textSecondary,
                                    fontWeight: FontWeight.w500,
                                  ),
                                ),
                                const SizedBox(height: 2),
                                Text(
                                  _topCategory!,
                                  style: TextStyle(
                                    fontSize: 15,
                                    fontWeight: FontWeight.w700,
                                    color: CategoryHelper.getColor(_topCategory),
                                  ),
                                ),
                              ],
                            ),
                            const Spacer(),
                            Text(
                              '${_currencyFormat.format(_categoryTotal(_topCategory!))} TL',
                              style: TextStyle(
                                fontSize: 14,
                                fontWeight: FontWeight.w700,
                                color: CategoryHelper.getColor(_topCategory),
                              ),
                            ),
                          ],
                        ),
                      ),

                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          'Son Fişlerin',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w700,
                            color: AppColors.txt(context),
                          ),
                        ),
                        TextButton(
                          onPressed: () async {
                            await Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (context) => const ReceiptListScreen(),
                              ),
                            );
                            _loadReceipts();
                          },
                          child: Text('Tümünü gör'),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),

                    _isLoading
                        ? const Center(
                            child: Padding(
                              padding: EdgeInsets.all(40),
                              child: CircularProgressIndicator(
                                color: AppColors.primary,
                              ),
                            ),
                          )
                        : _recentReceipts.isEmpty
                            ? Center(
                                child: Padding(
                                  padding: const EdgeInsets.symmetric(vertical: 40),
                                  child: Column(
                                    children: [
                                      Icon(Icons.receipt_long_outlined, size: 80, color: AppColors.primary),
                                      const SizedBox(height: 8),
                                      Text(
                                        'Henüz fiş eklemedin',
                                        style: TextStyle(
                                          fontSize: 16,
                                          fontWeight: FontWeight.w600,
                                          color: AppColors.txt(context),
                                        ),
                                      ),
                                      const SizedBox(height: 6),
                                      Text(
                                        'İlk fişini eklemek için "Fiş Ekle"\nbutonuna dokun',
                                        textAlign: TextAlign.center,
                                        style: TextStyle(
                                          fontSize: 13,
                                          color: AppColors.textSecondary,
                                          height: 1.5,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              )
                            : Column(
                                children: _recentReceipts.map((receipt) {
                                  return Container(
                                    margin: const EdgeInsets.only(bottom: 10),
                                    padding: const EdgeInsets.all(14),
                                    decoration: BoxDecoration(
                                      color: AppColors.surf(context),
                                      borderRadius: BorderRadius.circular(14),
                                      border: Border.all(color: AppColors.border),
                                    ),
                                    child: Row(
                                      children: [
                                        Container(
                                          padding: const EdgeInsets.all(10),
                                          decoration: BoxDecoration(
                                            color: CategoryHelper.getColor(receipt.categoryName).withOpacity(0.12),
                                            borderRadius: BorderRadius.circular(12),
                                          ),
                                          child: Icon(
                                            CategoryHelper.getIcon(receipt.categoryName),
                                            color: CategoryHelper.getColor(receipt.categoryName),
                                            size: 22,
                                          ),
                                        ),
                                        const SizedBox(width: 12),
                                        Expanded(
                                          child: Column(
                                            crossAxisAlignment: CrossAxisAlignment.start,
                                            children: [
                                              Text(
                                                receipt.storeName,
                                                style: TextStyle(
                                                  fontWeight: FontWeight.w600,
                                                  fontSize: 15,
                                                  color: AppColors.txt(context),
                                                ),
                                              ),
                                              const SizedBox(height: 4),
                                              Row(
                                                children: [
                                                  Container(
                                                    padding: const EdgeInsets.symmetric(
                                                      horizontal: 8,
                                                      vertical: 2,
                                                    ),
                                                    decoration: BoxDecoration(
                                                      color: CategoryHelper.getColor(receipt.categoryName).withOpacity(0.1),
                                                      borderRadius: BorderRadius.circular(6),
                                                    ),
                                                    child: Text(
                                                      receipt.categoryName ?? 'Kategorisiz',
                                                      style: TextStyle(
                                                        fontSize: 11,
                                                        fontWeight: FontWeight.w600,
                                                        color: CategoryHelper.getColor(receipt.categoryName),
                                                      ),
                                                    ),
                                                  ),
                                                  const SizedBox(width: 6),
                                                  Text(
                                                    DateFormatter.formatShort(receipt.receiptDate),
                                                    style: TextStyle(
                                                      fontSize: 11,
                                                      color: AppColors.textSecondary,
                                                    ),
                                                  ),
                                                ],
                                              ),
                                            ],
                                          ),
                                        ),
                                        Text(
                                          '${_currencyFormat.format(receipt.totalAmount)} TL',
                                          style: TextStyle(
                                            fontWeight: FontWeight.w800,
                                            fontSize: 15,
                                            color: AppColors.primary,
                                          ),
                                        ),
                                      ],
                                    ),
                                  );
                                }).toList(),
                              ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _QuickActionCard extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;
  final VoidCallback onTap;

  const _QuickActionCard({
    required this.icon,
    required this.label,
    required this.color,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 20, horizontal: 16),
        decoration: BoxDecoration(
          color: AppColors.surf(context),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: AppColors.border),
          boxShadow: [
            BoxShadow(
              color: color.withOpacity(0.08),
              blurRadius: 12,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: color.withOpacity(0.12),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Icon(icon, color: color, size: 20),
            ),
            const SizedBox(width: 10),
            Text(
              label,
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: AppColors.txt(context),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
