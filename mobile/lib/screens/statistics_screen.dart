import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import '../services/receipt_service.dart';
import '../models/receipt.dart';

class StatisticsScreen extends StatefulWidget {
  const StatisticsScreen({super.key});

  @override
  State<StatisticsScreen> createState() => _StatisticsScreenState();
}

class _StatisticsScreenState extends State<StatisticsScreen> {
  List<Receipt> _receipts = [];
  bool _isLoading = true;
  int _touchedIndex = -1;

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
      debugPrint('İstatistik hata: $e');
    }
  }

  // Kategoriye göre harcama topla
  Map<String, double> get _categoryTotals {
    final Map<String, double> totals = {};
    for (final receipt in _receipts) {
      final category = receipt.categoryName ?? 'Diğer';
      totals[category] = (totals[category] ?? 0) + receipt.totalAmount;
    }
    return totals;
  }

  // Bu ayki toplam
  double get _thisMonthTotal {
    final now = DateTime.now();
    return _receipts
        .where((r) {
          try {
            final date = DateTime.parse(r.receiptDate);
            return date.year == now.year && date.month == now.month;
          } catch (_) {
            return false;
          }
        })
        .fold(0.0, (sum, r) => sum + r.totalAmount);
  }

  // Kategori renkleri
  Color _categoryColor(int index) {
    const colors = [
      Color(0xFF6C63FF),
      Color(0xFF00BFA6),
      Color(0xFFFF6B6B),
      Color(0xFFFFD93D),
      Color(0xFF6BCB77),
      Color(0xFF4D96FF),
      Color(0xFFFF922B),
      Color(0xFFCC5DE8),
    ];
    return colors[index % colors.length];
  }

  @override
  Widget build(BuildContext context) {
    final totals = _categoryTotals;
    final categories = totals.keys.toList();
    final totalSpend = totals.values.fold(0.0, (a, b) => a + b);

    return Scaffold(
      backgroundColor: const Color(0xFFF8F7FF),
      appBar: AppBar(
        title: const Text('İstatistikler'),
        backgroundColor: const Color(0xFF6C63FF),
        foregroundColor: Colors.white,
      ),
      body: _isLoading
          ? const Center(
              child: CircularProgressIndicator(color: Color(0xFF6C63FF)),
            )
          : _receipts.isEmpty
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Container(
                        padding: const EdgeInsets.all(24),
                        decoration: BoxDecoration(
                          color: const Color(0xFF6C63FF).withOpacity(0.08),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          Icons.bar_chart_outlined,
                          size: 48,
                          color: Color(0xFF6C63FF),
                        ),
                      ),
                      const SizedBox(height: 16),
                      const Text(
                        'Henüz veri yok',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(height: 8),
                      const Text(
                        'Fiş ekledikçe istatistikler burada görünecek',
                        style: TextStyle(
                          fontSize: 13,
                          color: Color(0xFF9E9EBF),
                        ),
                      ),
                    ],
                  ),
                )
              : RefreshIndicator(
                  onRefresh: _loadReceipts,
                  child: SingleChildScrollView(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        // Toplam harcama kartı
                        Container(
                          padding: const EdgeInsets.all(20),
                          decoration: BoxDecoration(
                            gradient: const LinearGradient(
                              colors: [Color(0xFF6C63FF), Color(0xFF9C8FFF)],
                              begin: Alignment.topLeft,
                              end: Alignment.bottomRight,
                            ),
                            borderRadius: BorderRadius.circular(16),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text(
                                'Toplam Harcama',
                                style: TextStyle(
                                  color: Colors.white70,
                                  fontSize: 13,
                                ),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                '${totalSpend.toStringAsFixed(2).replaceAll('.', ',')} TL',
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 32,
                                  fontWeight: FontWeight.w800,
                                ),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                'Bu ay: ${_thisMonthTotal.toStringAsFixed(2).replaceAll('.', ',')} TL',
                                style: const TextStyle(
                                  color: Colors.white70,
                                  fontSize: 13,
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 24),

                        // Pasta grafik
                        const Text(
                          'Kategoriye Göre Dağılım',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w700,
                            color: Color(0xFF1A1A2E),
                          ),
                        ),
                        const SizedBox(height: 16),
                        Container(
                          padding: const EdgeInsets.all(20),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(color: const Color(0xFFEEEEF5)),
                          ),
                          child: Column(
                            children: [
                              SizedBox(
                                height: 220,
                                child: PieChart(
                                  PieChartData(
                                    pieTouchData: PieTouchData(
                                      touchCallback: (event, response) {
                                        setState(() {
                                          if (!event.isInterestedForInteractions ||
                                              response == null ||
                                              response.touchedSection == null) {
                                            _touchedIndex = -1;
                                            return;
                                          }
                                          _touchedIndex = response
                                              .touchedSection!
                                              .touchedSectionIndex;
                                        });
                                      },
                                    ),
                                    sections: categories
                                        .asMap()
                                        .entries
                                        .map((entry) {
                                      final index = entry.key;
                                      final category = entry.value;
                                      final value = totals[category]!;
                                      final percentage =
                                          (value / totalSpend * 100);
                                      final isTouched =
                                          index == _touchedIndex;

                                      return PieChartSectionData(
                                        value: value,
                                        title: isTouched
                                            ? '%${percentage.toStringAsFixed(1)}'
                                            : '',
                                        color: _categoryColor(index),
                                        radius: isTouched ? 80 : 65,
                                        titleStyle: const TextStyle(
                                          fontSize: 13,
                                          fontWeight: FontWeight.w700,
                                          color: Colors.white,
                                        ),
                                      );
                                    }).toList(),
                                    sectionsSpace: 2,
                                    centerSpaceRadius: 40,
                                  ),
                                ),
                              ),
                              const SizedBox(height: 16),
                              // Legend
                              Wrap(
                                spacing: 16,
                                runSpacing: 8,
                                children: categories
                                    .asMap()
                                    .entries
                                    .map((entry) {
                                  final index = entry.key;
                                  final category = entry.value;
                                  final value = totals[category]!;
                                  return Row(
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      Container(
                                        width: 12,
                                        height: 12,
                                        decoration: BoxDecoration(
                                          color: _categoryColor(index),
                                          shape: BoxShape.circle,
                                        ),
                                      ),
                                      const SizedBox(width: 6),
                                      Text(
                                        '$category: ${value.toStringAsFixed(2)} TL',
                                        style: const TextStyle(
                                          fontSize: 12,
                                          color: Color(0xFF1A1A2E),
                                        ),
                                      ),
                                    ],
                                  );
                                }).toList(),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 24),

                        // Kategori listesi
                        const Text(
                          'Kategori Detayları',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w700,
                            color: Color(0xFF1A1A2E),
                          ),
                        ),
                        const SizedBox(height: 12),
                        ...categories.asMap().entries.map((entry) {
                          final index = entry.key;
                          final category = entry.value;
                          final value = totals[category]!;
                          final percentage = value / totalSpend * 100;

                          return Container(
                            margin: const EdgeInsets.only(bottom: 10),
                            padding: const EdgeInsets.all(16),
                            decoration: BoxDecoration(
                              color: Colors.white,
                              borderRadius: BorderRadius.circular(14),
                              border:
                                  Border.all(color: const Color(0xFFEEEEF5)),
                            ),
                            child: Row(
                              children: [
                                Container(
                                  width: 40,
                                  height: 40,
                                  decoration: BoxDecoration(
                                    color: _categoryColor(index)
                                        .withOpacity(0.12),
                                    borderRadius: BorderRadius.circular(10),
                                  ),
                                  child: Icon(
                                    Icons.category_outlined,
                                    color: _categoryColor(index),
                                    size: 20,
                                  ),
                                ),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        category,
                                        style: const TextStyle(
                                          fontWeight: FontWeight.w600,
                                          fontSize: 14,
                                        ),
                                      ),
                                      const SizedBox(height: 4),
                                      ClipRRect(
                                        borderRadius: BorderRadius.circular(4),
                                        child: LinearProgressIndicator(
                                          value: percentage / 100,
                                          backgroundColor:
                                              const Color(0xFFEEEEF5),
                                          valueColor:
                                              AlwaysStoppedAnimation<Color>(
                                            _categoryColor(index),
                                          ),
                                          minHeight: 6,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                                const SizedBox(width: 12),
                                Column(
                                  crossAxisAlignment: CrossAxisAlignment.end,
                                  children: [
                                    Text(
                                      '${value.toStringAsFixed(2)} TL',
                                      style: TextStyle(
                                        fontWeight: FontWeight.w700,
                                        fontSize: 14,
                                        color: _categoryColor(index),
                                      ),
                                    ),
                                    Text(
                                      '%${percentage.toStringAsFixed(1)}',
                                      style: const TextStyle(
                                        fontSize: 12,
                                        color: Color(0xFF9E9EBF),
                                      ),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          );
                        }),
                      ],
                    ),
                  ),
                ),
    );
  }
}
