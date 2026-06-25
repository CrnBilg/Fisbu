import 'package:intl/intl.dart';

class DateFormatter {
  DateFormatter._();

  static final _turkishMonths = {
    1: 'Oca', 2: 'Şub', 3: 'Mar', 4: 'Nis',
    5: 'May', 6: 'Haz', 7: 'Tem', 8: 'Ağu',
    9: 'Eyl', 10: 'Eki', 11: 'Kas', 12: 'Ara',
  };

  static final _turkishMonthsFull = {
    1: 'Ocak', 2: 'Şubat', 3: 'Mart', 4: 'Nisan',
    5: 'Mayıs', 6: 'Haziran', 7: 'Temmuz', 8: 'Ağustos',
    9: 'Eylül', 10: 'Ekim', 11: 'Kasım', 12: 'Aralık',
  };

  // "2026-06-21" → "21 Haz 2026"
  static String formatShort(String? dateStr) {
    if (dateStr == null || dateStr.isEmpty) return '';
    try {
      final date = DateTime.parse(dateStr);
      return '${date.day} ${_turkishMonths[date.month]} ${date.year}';
    } catch (_) {
      return dateStr;
    }
  }

  // "2026-06-21" → "21 Haziran 2026"
  static String formatLong(String? dateStr) {
    if (dateStr == null || dateStr.isEmpty) return '';
    try {
      final date = DateTime.parse(dateStr);
      return '${date.day} ${_turkishMonthsFull[date.month]} ${date.year}';
    } catch (_) {
      return dateStr;
    }
  }
}