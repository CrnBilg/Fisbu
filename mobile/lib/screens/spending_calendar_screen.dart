import 'package:flutter/material.dart';
import 'package:table_calendar/table_calendar.dart';
import 'package:intl/intl.dart';
import '../models/receipt.dart';
import '../services/receipt_service.dart';
import '../core/theme/app_colors.dart';
import '../core/utils/category_helper.dart';
import '../core/utils/date_formatter.dart';
import '../core/utils/network_error.dart';
import 'receipt_detail_screen.dart';

class SpendingCalendarScreen extends StatefulWidget {
  const SpendingCalendarScreen({super.key});

  @override
  State<SpendingCalendarScreen> createState() => _SpendingCalendarScreenState();
}

class _SpendingCalendarScreenState extends State<SpendingCalendarScreen> {
  final _currencyFormat = NumberFormat('#,##0.00', 'tr_TR');
  bool _isLoading = true;
  String? _errorMessage;
  Map<DateTime, List<Receipt>> _receiptsByDay = {};
  double _maxDaySpend = 0;
  DateTime _focusedDay = DateTime.now();
  DateTime? _selectedDay;

  @override
  void initState() {
    super.initState();
    _selectedDay = DateTime.now();
    _loadReceipts();
  }

  DateTime _normalize(DateTime d) => DateTime(d.year, d.month, d.day);

  Future<void> _loadReceipts() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final receipts = await ReceiptService.getReceipts();
      final byDay = <DateTime, List<Receipt>>{};
      final totalByDay = <DateTime, double>{};
      for (final receipt in receipts) {
        try {
          final day = _normalize(DateTime.parse(receipt.receiptDate));
          byDay.putIfAbsent(day, () => []).add(receipt);
          totalByDay[day] = (totalByDay[day] ?? 0) + receipt.totalAmount;
        } catch (_) {}
      }
      setState(() {
        _receiptsByDay = byDay;
        _maxDaySpend = totalByDay.values.isEmpty ? 0 : totalByDay.values.reduce((a, b) => a > b ? a : b);
      });
    } catch (e) {
      setState(() => _errorMessage = NetworkError.friendlyMessage(e, fallback: 'Fişler alınamadı, lütfen tekrar deneyin.'));
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  List<Receipt> _receiptsForDay(DateTime day) => _receiptsByDay[_normalize(day)] ?? [];

  double _totalForDay(DateTime day) =>
      _receiptsForDay(day).fold(0.0, (sum, r) => sum + r.totalAmount);

  Color _heatColor(DateTime day, bool isDark) {
    final total = _totalForDay(day);
    if (total <= 0 || _maxDaySpend <= 0) return Colors.transparent;
    final intensity = (total / _maxDaySpend).clamp(0.15, 1.0);
    return AppColors.primary.withOpacity(isDark ? intensity * 0.5 : intensity * 0.35);
  }

  Widget _buildDayCell(DateTime day, bool isDark, {bool isSelected = false, bool isToday = false}) {
    final hasSpend = _totalForDay(day) > 0;
    return Container(
      margin: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: isSelected ? AppColors.primary : _heatColor(day, isDark),
        shape: BoxShape.circle,
        border: isToday && !isSelected ? Border.all(color: AppColors.primary, width: 1.5) : null,
      ),
      alignment: Alignment.center,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            '${day.day}',
            style: TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: isSelected ? Colors.white : AppColors.txt(context),
            ),
          ),
          if (hasSpend && !isSelected)
            Container(
              margin: const EdgeInsets.only(top: 1),
              width: 4,
              height: 4,
              decoration: const BoxDecoration(color: AppColors.primary, shape: BoxShape.circle),
            ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final selectedReceipts = _selectedDay != null ? _receiptsForDay(_selectedDay!) : <Receipt>[];
    final selectedTotal = _selectedDay != null ? _totalForDay(_selectedDay!) : 0.0;

    return Scaffold(
      appBar: AppBar(title: const Text('Harcama Takvimi')),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: AppColors.primary))
          : _errorMessage != null
              ? Center(child: Text(_errorMessage!, style: TextStyle(color: AppColors.textSecondary)))
              : RefreshIndicator(
                  onRefresh: _loadReceipts,
                  child: ListView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.all(16),
                    children: [
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: AppColors.surf(context),
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(color: AppColors.brd(context)),
                        ),
                        child: TableCalendar<Receipt>(
                          locale: 'tr_TR',
                          firstDay: DateTime(2020, 1, 1),
                          lastDay: DateTime.now().add(const Duration(days: 1)),
                          focusedDay: _focusedDay,
                          selectedDayPredicate: (day) => _selectedDay != null && isSameDay(_selectedDay!, day),
                          eventLoader: _receiptsForDay,
                          headerStyle: const HeaderStyle(formatButtonVisible: false, titleCentered: true),
                          calendarStyle: const CalendarStyle(outsideDaysVisible: false),
                          onDaySelected: (selected, focused) {
                            setState(() {
                              _selectedDay = selected;
                              _focusedDay = focused;
                            });
                          },
                          onPageChanged: (focused) => setState(() => _focusedDay = focused),
                          calendarBuilders: CalendarBuilders(
                            defaultBuilder: (context, day, _) => _buildDayCell(day, isDark),
                            todayBuilder: (context, day, _) => _buildDayCell(day, isDark, isToday: true),
                            selectedBuilder: (context, day, _) => _buildDayCell(day, isDark, isSelected: true),
                          ),
                        ),
                      ),
                      const SizedBox(height: 20),
                      if (_selectedDay != null) ...[
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              DateFormatter.formatLong(_selectedDay!.toIso8601String()),
                              style: TextStyle(fontSize: 15, fontWeight: FontWeight.w700, color: AppColors.txt(context)),
                            ),
                            if (selectedTotal > 0)
                              Text('${_currencyFormat.format(selectedTotal)} TL',
                                  style: const TextStyle(fontWeight: FontWeight.w800, color: AppColors.primary)),
                          ],
                        ),
                        const SizedBox(height: 12),
                        if (selectedReceipts.isEmpty)
                          Padding(
                            padding: const EdgeInsets.symmetric(vertical: 20),
                            child: Center(
                              child: Text('Bu gün fiş yok', style: TextStyle(color: AppColors.textSecondary)),
                            ),
                          )
                        else
                          ...selectedReceipts.map((receipt) => GestureDetector(
                                onTap: () async {
                                  final result = await Navigator.push(
                                    context,
                                    MaterialPageRoute(
                                      builder: (context) => ReceiptDetailScreen(receipt: receipt),
                                    ),
                                  );
                                  if (result == true) _loadReceipts();
                                },
                                child: Container(
                                  margin: const EdgeInsets.only(bottom: 10),
                                  padding: const EdgeInsets.all(14),
                                  decoration: BoxDecoration(
                                    color: AppColors.surf(context),
                                    borderRadius: BorderRadius.circular(14),
                                    border: Border.all(color: AppColors.brd(context)),
                                  ),
                                  child: Row(
                                    children: [
                                      Container(
                                        padding: const EdgeInsets.all(10),
                                        decoration: BoxDecoration(
                                          color: CategoryHelper.getColor(receipt.categoryName).withOpacity(0.12),
                                          borderRadius: BorderRadius.circular(10),
                                        ),
                                        child: Icon(CategoryHelper.getIcon(receipt.categoryName),
                                            color: CategoryHelper.getColor(receipt.categoryName), size: 18),
                                      ),
                                      const SizedBox(width: 12),
                                      Expanded(
                                        child: Text(receipt.storeName,
                                            style: TextStyle(fontWeight: FontWeight.w600, color: AppColors.txt(context)),
                                            maxLines: 1, overflow: TextOverflow.ellipsis),
                                      ),
                                      Text('${_currencyFormat.format(receipt.totalAmount)} TL',
                                          style: const TextStyle(fontWeight: FontWeight.w700, color: AppColors.primary)),
                                    ],
                                  ),
                                ),
                              )),
                      ],
                    ],
                  ),
                ),
    );
  }
}
