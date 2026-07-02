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

  // Aylık filtre
  late DateTime _selectedMonth;
  List<DateTime> _availableMonths = [];

  @override
  void initState() {
    super.initState();
    _selectedMonth = DateTime(DateTime.now().year, DateTime.now().month);
    _availableMonths = [_selectedMonth];
    _loadReceipts();
  }

  Future<void> _loadReceipts() async {
    try {
      final receipts = await ReceiptService.getReceipts();
      final months = <DateTime>{};
      for (final r in receipts) {
        try {
          final d = DateTime.parse(r.receiptDate);
          months.add(DateTime(d.year, d.month));
        } catch (_) {}
      }
      final sortedMonths = months.toList()
        ..sort((a, b) => b.compareTo(a));
      final fallbackMonth = DateTime(DateTime.now().year, DateTime.now().month);

      setState(() {
        _receipts = receipts;
        _availableMonths = sortedMonths.isEmpty
            ? [fallbackMonth]
            : sortedMonths;
        _selectedMonth = sortedMonths.isEmpty
            ? fallbackMonth
            : sortedMonths.first;
        _isLoading = false;
      });
    } catch (e) {
      debugPrint('İstatistik hata: $e');
      setState(() => _isLoading = false);
    }
  }

  // Seçili aya göre filtrele
  List<Receipt> get _filteredReceipts {
    return _receipts.where((r) {
      try {
        final d = DateTime.parse(r.receiptDate);
        return d.year == _selectedMonth.year &&
            d.month == _selectedMonth.month;
      } catch (_) {
        return false;
      }
    }).toList();
  }

  // Kategoriye göre topla
  Map<String, double> get _categoryTotals {
    final Map<String, double> totals = {};
    for (final r in _filteredReceipts) {
      final cat = r.categoryName ?? 'Diğer';
      totals[cat] = (totals[cat] ?? 0) + r.totalAmount;
    }
    return totals;
  }

  double get _totalSpend =>
      _filteredReceipts.fold(0.0, (s, r) => s + r.totalAmount);

  double get _avgAmount => _filteredReceipts.isEmpty
      ? 0
      : _totalSpend / _filteredReceipts.length;

  String get _topCategory {
    if (_categoryTotals.isEmpty) return '-';
    return _categoryTotals.entries
        .reduce((a, b) => a.value > b.value ? a : b)
        .key;
  }

  double get _maxReceipt => _filteredReceipts.isEmpty
      ? 0
      : _filteredReceipts
          .map((r) => r.totalAmount)
          .reduce((a, b) => a > b ? a : b);

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

  String _monthLabel(DateTime d) {
    const months = [
      'Ocak', 'Şubat', 'Mart', 'Nisan', 'Mayıs', 'Haziran',
      'Temmuz', 'Ağustos', 'Eylül', 'Ekim', 'Kasım', 'Aralık'
    ];
    return '${months[d.month - 1]} ${d.year}';
  }

  Map<String, double> get _allCategoryTotals {
    final totals = <String, double>{};
    for (final receipt in _receipts) {
      final category = receipt.categoryName ?? 'Diğer';
      totals[category] = (totals[category] ?? 0) + receipt.totalAmount;
    }
    return totals;
  }

  Map<DateTime, double> get _monthlyTotals {
    final totals = <DateTime, double>{};
    for (final receipt in _receipts) {
      try {
        final date = DateTime.parse(receipt.receiptDate);
        final month = DateTime(date.year, date.month);
        totals[month] = (totals[month] ?? 0) + receipt.totalAmount;
      } catch (_) {}
    }
    final entries = totals.entries.toList()
      ..sort((a, b) => a.key.compareTo(b.key));
    return Map<DateTime, double>.fromEntries(entries);
  }

  double get _allTimeTotal =>
      _receipts.fold(0.0, (sum, receipt) => sum + receipt.totalAmount);

  Widget _buildGeneralStatistics(bool isDark) {
    final categoryTotals = _allCategoryTotals;
    final categories = categoryTotals.keys.toList();
    final monthlyTotals = _monthlyTotals;
    final months = monthlyTotals.keys.toList();
    final totalSpend = _allTimeTotal;
    final surfaceColor = isDark ? const Color(0xFF2A2A3E) : Colors.white;
    final titleColor = isDark ? Colors.white : const Color(0xFF1A1A2E);

    Widget emptyCard(String message) {
      return Container(
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
          color: surfaceColor,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: const Color(0xFFEEEEF5)),
        ),
        child: Center(
          child: Text(
            message,
            style: const TextStyle(color: Color(0xFF9E9EBF)),
          ),
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadReceipts,
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
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
                    'Tüm Zamanlar Toplam Harcama',
                    style: TextStyle(color: Colors.white70, fontSize: 13),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    '${totalSpend.toStringAsFixed(2).replaceAll('.', ',')} TL',
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 30,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            Text(
              'Aylık Harcamalar',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: titleColor,
              ),
            ),
            const SizedBox(height: 12),
            months.isEmpty
                ? emptyCard('Henüz aylık veri yok')
                : Container(
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      color: surfaceColor,
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: const Color(0xFFEEEEF5)),
                    ),
                    child: SizedBox(
                      height: 240,
                      child: BarChart(
                        BarChartData(
                          alignment: BarChartAlignment.spaceAround,
                          maxY: monthlyTotals.values.reduce(
                                    (a, b) => a > b ? a : b,
                                  ) <=
                                  0
                              ? 100
                              : monthlyTotals.values.reduce(
                                    (a, b) => a > b ? a : b,
                                  ) *
                                  1.2,
                          barTouchData: BarTouchData(
                            enabled: true,
                            touchTooltipData: BarTouchTooltipData(
                              getTooltipItem: (group, groupIndex, rod, rodIndex) {
                                return BarTooltipItem(
                                  '${_monthLabel(months[groupIndex])}\n${rod.toY.toStringAsFixed(2)} TL',
                                  const TextStyle(
                                    color: Colors.white,
                                    fontSize: 12,
                                  ),
                                );
                              },
                            ),
                          ),
                          titlesData: FlTitlesData(
                            show: true,
                            bottomTitles: AxisTitles(
                              sideTitles: SideTitles(
                                showTitles: true,
                                getTitlesWidget: (value, meta) {
                                  final index = value.toInt();
                                  if (index < 0 || index >= months.length) {
                                    return const SizedBox();
                                  }
                                  return Padding(
                                    padding: const EdgeInsets.only(top: 6),
                                    child: Text(
                                      _monthLabel(months[index]),
                                      textAlign: TextAlign.center,
                                      style: TextStyle(
                                        fontSize: 9,
                                        color: isDark
                                            ? Colors.white70
                                            : const Color(0xFF9E9EBF),
                                      ),
                                    ),
                                  );
                                },
                              ),
                            ),
                            leftTitles: const AxisTitles(
                              sideTitles: SideTitles(showTitles: false),
                            ),
                            topTitles: const AxisTitles(
                              sideTitles: SideTitles(showTitles: false),
                            ),
                            rightTitles: const AxisTitles(
                              sideTitles: SideTitles(showTitles: false),
                            ),
                          ),
                          gridData: const FlGridData(show: false),
                          borderData: FlBorderData(show: false),
                          barGroups: months.asMap().entries.map((entry) {
                            return BarChartGroupData(
                              x: entry.key,
                              barRods: [
                                BarChartRodData(
                                  toY: monthlyTotals[entry.value]!,
                                  color: _categoryColor(entry.key),
                                  width: 20,
                                  borderRadius: BorderRadius.circular(6),
                                ),
                              ],
                            );
                          }).toList(),
                        ),
                      ),
                    ),
                  ),
            const SizedBox(height: 24),
            Text(
              'Kategoriye Göre Dağılım',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: titleColor,
              ),
            ),
            const SizedBox(height: 12),
            categories.isEmpty
                ? emptyCard('Henüz kategori verisi yok')
                : Container(
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      color: surfaceColor,
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: const Color(0xFFEEEEF5)),
                    ),
                    child: Column(
                      children: [
                        SizedBox(
                          height: 220,
                          child: PieChart(
                            PieChartData(
                              sections: categories.asMap().entries.map((entry) {
                                return PieChartSectionData(
                                  value: categoryTotals[entry.value]!,
                                  title: '',
                                  color: _categoryColor(entry.key),
                                  radius: 65,
                                );
                              }).toList(),
                              sectionsSpace: 2,
                              centerSpaceRadius: 40,
                            ),
                          ),
                        ),
                        const SizedBox(height: 16),
                        Wrap(
                          spacing: 16,
                          runSpacing: 8,
                          children: categories.asMap().entries.map((entry) {
                            final value = categoryTotals[entry.value]!;
                            return Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Container(
                                  width: 12,
                                  height: 12,
                                  decoration: BoxDecoration(
                                    color: _categoryColor(entry.key),
                                    shape: BoxShape.circle,
                                  ),
                                ),
                                const SizedBox(width: 6),
                                Text(
                                  '${entry.value}: ${value.toStringAsFixed(2)} TL',
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: isDark
                                        ? Colors.white70
                                        : const Color(0xFF1A1A2E),
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
            Text(
              'Kategori Detayları',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: titleColor,
              ),
            ),
            const SizedBox(height: 12),
            if (categories.isEmpty)
              emptyCard('Henüz kategori verisi yok')
            else
              ...categories.asMap().entries.map((entry) {
                final category = entry.value;
                final value = categoryTotals[category]!;
                final percentage = totalSpend > 0 ? value / totalSpend * 100 : 0;
                return Container(
                  margin: const EdgeInsets.only(bottom: 10),
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: surfaceColor,
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(color: const Color(0xFFEEEEF5)),
                  ),
                  child: Row(
                    children: [
                      Container(
                        width: 40,
                        height: 40,
                        decoration: BoxDecoration(
                          color: _categoryColor(entry.key).withOpacity(0.12),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Icon(
                          Icons.category_outlined,
                          color: _categoryColor(entry.key),
                          size: 20,
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              category,
                              style: TextStyle(
                                fontWeight: FontWeight.w600,
                                fontSize: 14,
                                color: titleColor,
                              ),
                            ),
                            const SizedBox(height: 4),
                            ClipRRect(
                              borderRadius: BorderRadius.circular(4),
                              child: LinearProgressIndicator(
                                value: percentage / 100,
                                backgroundColor: const Color(0xFFEEEEF5),
                                valueColor: AlwaysStoppedAnimation<Color>(
                                  _categoryColor(entry.key),
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
                              color: _categoryColor(entry.key),
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
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final totals = _categoryTotals;
    final categories = totals.keys.toList();
    final currentMonth = DateTime(DateTime.now().year, DateTime.now().month);
    final dropdownMonths = _availableMonths.isEmpty
        ? <DateTime>[currentMonth]
        : _availableMonths;
    final dropdownValue = dropdownMonths.contains(_selectedMonth)
        ? _selectedMonth
        : dropdownMonths.first;

    return DefaultTabController(
      length: 2,
      child: Scaffold(
        backgroundColor:
            isDark ? const Color(0xFF1A1A2E) : const Color(0xFFF8F7FF),
        appBar: AppBar(
          title: const Text('İstatistikler'),
          backgroundColor: const Color(0xFF6C63FF),
          foregroundColor: Colors.white,
          bottom: const TabBar(
            indicatorColor: Colors.white,
            labelColor: Colors.white,
            unselectedLabelColor: Colors.white70,
            tabs: [
              Tab(text: 'Aylık İstatistik'),
              Tab(text: 'Genel İstatistik'),
            ],
          ),
        ),
        body: _isLoading
            ? const Center(
                child: CircularProgressIndicator(color: Color(0xFF6C63FF)),
              )
            : TabBarView(
                children: [
                  RefreshIndicator(
                    onRefresh: _loadReceipts,
                    child: SingleChildScrollView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        // Aylık filtre dropdown
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 16, vertical: 4),
                          decoration: BoxDecoration(
                            color: isDark ? const Color(0xFF2A2A3E) : Colors.white,
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(
                                color: const Color(0xFF6C63FF).withOpacity(0.3)),
                          ),
                          child: DropdownButtonHideUnderline(
                            child: DropdownButton<DateTime>(
                              value: dropdownValue,
                              isExpanded: true,
                              dropdownColor: isDark
                                  ? const Color(0xFF2A2A3E)
                                  : Colors.white,
                              style: TextStyle(
                                color: isDark
                                    ? Colors.white
                                    : const Color(0xFF1A1A2E),
                                fontWeight: FontWeight.w600,
                                fontSize: 15,
                              ),
                              icon: const Icon(Icons.keyboard_arrow_down,
                                  color: Color(0xFF6C63FF)),
                              items: dropdownMonths.map((month) {
                                return DropdownMenuItem(
                                  value: month,
                                  child: Text(_monthLabel(month)),
                                );
                              }).toList(),
                              onChanged: (val) {
                                if (val != null) {
                                  setState(() {
                                    _selectedMonth = val;
                                    _touchedIndex = -1;
                                  });
                                }
                              },
                            ),
                          ),
                        ),
                        const SizedBox(height: 16),

                        // Özet kartlar
                        Row(
                          children: [
                            Expanded(
                              child: _SummaryCard(
                                label: 'Toplam',
                                value:
                                    '${_totalSpend.toStringAsFixed(2).replaceAll('.', ',')} TL',
                                icon: Icons.account_balance_wallet_outlined,
                                color: const Color(0xFF6C63FF),
                                isDark: isDark,
                              ),
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: _SummaryCard(
                                label: 'Ortalama',
                                value:
                                    '${_avgAmount.toStringAsFixed(2).replaceAll('.', ',')} TL',
                                icon: Icons.calculate_outlined,
                                color: const Color(0xFF00BFA6),
                                isDark: isDark,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 10),
                        Row(
                          children: [
                            Expanded(
                              child: _SummaryCard(
                                label: 'En Yüksek',
                                value:
                                    '${_maxReceipt.toStringAsFixed(2).replaceAll('.', ',')} TL',
                                icon: Icons.trending_up,
                                color: const Color(0xFFFF6B6B),
                                isDark: isDark,
                              ),
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: _SummaryCard(
                                label: 'En Çok Kategori',
                                value: _topCategory,
                                icon: Icons.category_outlined,
                                color: const Color(0xFFFF922B),
                                isDark: isDark,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 24),

                        // Pasta grafik
                        Text(
                          'Kategoriye Göre Dağılım',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w700,
                            color: isDark
                                ? Colors.white
                                : const Color(0xFF1A1A2E),
                          ),
                        ),
                        const SizedBox(height: 12),
                        _filteredReceipts.isEmpty
                            ? Container(
                                padding: const EdgeInsets.all(24),
                                decoration: BoxDecoration(
                                  color: isDark
                                      ? const Color(0xFF2A2A3E)
                                      : Colors.white,
                                  borderRadius: BorderRadius.circular(16),
                                ),
                                child: const Center(
                                  child: Text(
                                    'Bu ay fiş yok',
                                    style:
                                        TextStyle(color: Color(0xFF9E9EBF)),
                                  ),
                                ),
                              )
                            : Container(
                                padding: const EdgeInsets.all(20),
                                decoration: BoxDecoration(
                                  color: isDark
                                      ? const Color(0xFF2A2A3E)
                                      : Colors.white,
                                  borderRadius: BorderRadius.circular(16),
                                  border: Border.all(
                                      color: const Color(0xFFEEEEF5)),
                                ),
                                child: Column(
                                  children: [
                                    SizedBox(
                                      height: 220,
                                      child: PieChart(
                                        PieChartData(
                                          pieTouchData: PieTouchData(
                                            touchCallback:
                                                (event, response) {
                                              setState(() {
                                                if (!event
                                                        .isInterestedForInteractions ||
                                                    response == null ||
                                                    response.touchedSection ==
                                                        null) {
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
                                            final value =
                                                totals[category]!;
                                            final percentage =
                                                value / _totalSpend * 100;
                                            final isTouched =
                                                index == _touchedIndex;
                                            return PieChartSectionData(
                                              value: value,
                                              title: isTouched
                                                  ? '%${percentage.toStringAsFixed(1)}'
                                                  : '',
                                              color:
                                                  _categoryColor(index),
                                              radius:
                                                  isTouched ? 80 : 65,
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
                                                color:
                                                    _categoryColor(index),
                                                shape: BoxShape.circle,
                                              ),
                                            ),
                                            const SizedBox(width: 6),
                                            Text(
                                              '$category: ${value.toStringAsFixed(2)} TL',
                                              style: TextStyle(
                                                fontSize: 12,
                                                color: isDark
                                                    ? Colors.white70
                                                    : const Color(
                                                        0xFF1A1A2E),
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

                        // Bar chart
                        Text(
                          'Kategori Harcamaları',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w700,
                            color: isDark
                                ? Colors.white
                                : const Color(0xFF1A1A2E),
                          ),
                        ),
                        const SizedBox(height: 12),
                        _filteredReceipts.isEmpty
                            ? Container(
                                padding: const EdgeInsets.all(24),
                                decoration: BoxDecoration(
                                  color: isDark
                                      ? const Color(0xFF2A2A3E)
                                      : Colors.white,
                                  borderRadius: BorderRadius.circular(16),
                                  border: Border.all(
                                      color: const Color(0xFFEEEEF5)),
                                ),
                                child: const Center(
                                  child: Text(
                                    'Bu ay veri yok',
                                    style: TextStyle(
                                      color: Color(0xFF9E9EBF),
                                    ),
                                  ),
                                ),
                              )
                            : Container(
                                padding: const EdgeInsets.all(20),
                                decoration: BoxDecoration(
                                  color: isDark
                                      ? const Color(0xFF2A2A3E)
                                      : Colors.white,
                                  borderRadius: BorderRadius.circular(16),
                                  border: Border.all(
                                      color: const Color(0xFFEEEEF5)),
                                ),
                                child: SizedBox(
                                  height: 200,
                                  child: BarChart(
                                    BarChartData(
                                      alignment:
                                          BarChartAlignment.spaceAround,
                                      maxY: totals.values.isEmpty
                                          ? 100
                                          : totals.values.reduce(
                                                  (a, b) => a > b ? a : b) *
                                              1.2,
                                      barTouchData: BarTouchData(
                                        enabled: true,
                                        touchTooltipData:
                                            BarTouchTooltipData(
                                          getTooltipItem: (group, groupIndex,
                                              rod, rodIndex) {
                                            return BarTooltipItem(
                                              '${categories[groupIndex]}\n${rod.toY.toStringAsFixed(2)} TL',
                                              const TextStyle(
                                                color: Colors.white,
                                                fontSize: 12,
                                              ),
                                            );
                                          },
                                        ),
                                      ),
                                      titlesData: FlTitlesData(
                                        show: true,
                                        bottomTitles: AxisTitles(
                                          sideTitles: SideTitles(
                                            showTitles: true,
                                            getTitlesWidget: (value, meta) {
                                              final index = value.toInt();
                                              if (index < 0 ||
                                                  index >=
                                                      categories.length) {
                                                return const SizedBox();
                                              }
                                              final cat =
                                                  categories[index];
                                              return Padding(
                                                padding:
                                                    const EdgeInsets.only(
                                                        top: 6),
                                                child: Text(
                                                  cat.length > 6
                                                      ? '${cat.substring(0, 6)}..'
                                                      : cat,
                                                  style: TextStyle(
                                                    fontSize: 10,
                                                    color: isDark
                                                        ? Colors.white70
                                                        : const Color(
                                                            0xFF9E9EBF),
                                                  ),
                                                ),
                                              );
                                            },
                                          ),
                                        ),
                                        leftTitles: const AxisTitles(
                                          sideTitles:
                                              SideTitles(showTitles: false),
                                        ),
                                        topTitles: const AxisTitles(
                                          sideTitles:
                                              SideTitles(showTitles: false),
                                        ),
                                        rightTitles: const AxisTitles(
                                          sideTitles:
                                              SideTitles(showTitles: false),
                                        ),
                                      ),
                                      gridData: const FlGridData(show: false),
                                      borderData:
                                          FlBorderData(show: false),
                                      barGroups: categories
                                          .asMap()
                                          .entries
                                          .map((entry) {
                                        return BarChartGroupData(
                                          x: entry.key,
                                          barRods: [
                                            BarChartRodData(
                                              toY: totals[entry.value]!,
                                              color: _categoryColor(
                                                  entry.key),
                                              width: 20,
                                              borderRadius:
                                                  BorderRadius.circular(6),
                                            ),
                                          ],
                                        );
                                      }).toList(),
                                    ),
                                  ),
                                ),
                              ),
                        const SizedBox(height: 24),

                        // Kategori detay listesi
                        Text(
                          'Kategori Detayları',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w700,
                            color: isDark
                                ? Colors.white
                                : const Color(0xFF1A1A2E),
                          ),
                        ),
                        const SizedBox(height: 12),
                        ...categories.asMap().entries.map((entry) {
                          final index = entry.key;
                          final category = entry.value;
                          final value = totals[category]!;
                          final percentage =
                              _totalSpend > 0 ? value / _totalSpend * 100 : 0;
                          return Container(
                            margin: const EdgeInsets.only(bottom: 10),
                            padding: const EdgeInsets.all(16),
                            decoration: BoxDecoration(
                              color: isDark
                                  ? const Color(0xFF2A2A3E)
                                  : Colors.white,
                              borderRadius: BorderRadius.circular(14),
                              border: Border.all(
                                  color: const Color(0xFFEEEEF5)),
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
                                        style: TextStyle(
                                          fontWeight: FontWeight.w600,
                                          fontSize: 14,
                                          color: isDark
                                              ? Colors.white
                                              : const Color(0xFF1A1A2E),
                                        ),
                                      ),
                                      const SizedBox(height: 4),
                                      ClipRRect(
                                        borderRadius:
                                            BorderRadius.circular(4),
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
                                  crossAxisAlignment:
                                      CrossAxisAlignment.end,
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
                  _buildGeneralStatistics(isDark),
                ],
              ),
      ),
    );
  }
}

class _SummaryCard extends StatelessWidget {
  final String label;
  final String value;
  final IconData icon;
  final Color color;
  final bool isDark;

  const _SummaryCard({
    required this.label,
    required this.value,
    required this.icon,
    required this.color,
    required this.isDark,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: isDark ? const Color(0xFF2A2A3E) : Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: const Color(0xFFEEEEF5)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: color.withOpacity(0.12),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(icon, color: color, size: 18),
          ),
          const SizedBox(height: 10),
          Text(
            label,
            style: const TextStyle(
              fontSize: 12,
              color: Color(0xFF9E9EBF),
            ),
          ),
          const SizedBox(height: 4),
          Text(
            value,
            style: TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w700,
              color: isDark ? Colors.white : const Color(0xFF1A1A2E),
            ),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
        ],
      ),
    );
  }
}
