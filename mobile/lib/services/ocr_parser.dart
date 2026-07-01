class OcrParser {
  static double? extractAmount(String text) {
    final lines = text.split('\n');

    // TOPLAM/TUTAR içeren satırlarda ara
    final keywordPattern = RegExp(
      r'(?:GENEL TOPLAM|ÖDENECEK TUTAR|ÖDENECEK|TOPLAM TUTAR|TOPLAM|TUTAR|TOTAL)[^\d]*\*?(\d{1,3}(?:[.,]\d{3})*[.,]\d{2}|\d{1,6}[.,]\d{2})',
      caseSensitive: false,
    );

    for (final line in lines) {
      final match = keywordPattern.firstMatch(line);
      if (match != null) {
        final amount = _parseAmount(match.group(1)!);
        if (amount != null && amount > 0 && amount < 1000000) return amount;
      }
    }

    // TL ile biten satırlarda ara (KDV satırlarını atla)
    final tlPattern = RegExp(r'\*?(\d{1,3}(?:[.,]\d{3})*[.,]\d{2}|\d{1,6}[.,]\d{2})\s*TL', caseSensitive: false);
    double? largest;
    for (final line in lines) {
      final lower = line.toLowerCase();
      if (lower.contains('kdv') && !lower.contains('toplam')) continue;
      if (lower.contains('indirim')) continue;
      final match = tlPattern.firstMatch(line);
      if (match != null) {
        final amount = _parseAmount(match.group(1)!);
        if (amount != null && amount > 0 && amount < 1000000) {
          if (largest == null || amount > largest) largest = amount;
        }
      }
    }
    if (largest != null) return largest;

    // Son çare: en büyük sayıyı al
    final anyPattern = RegExp(r'\*?(\d{1,3}(?:[.,]\d{3})*[.,]\d{2}|\d{1,6}[.,]\d{2})');
    double? fallback;
    for (final line in lines) {
      final match = anyPattern.firstMatch(line);
      if (match != null) {
        final amount = _parseAmount(match.group(1)!);
        if (amount != null && amount > 0 && amount < 1000000) {
          if (fallback == null || amount > fallback) fallback = amount;
        }
      }
    }
    return fallback;
  }

  // Türk formatı: 2.400,00 → 2400.0 veya 2400,00 → 2400.0
  static double? _parseAmount(String raw) {
    // 1.234,56 formatı (Türk): nokta binlik, virgül ondalık
    if (raw.contains(',') && raw.contains('.')) {
      final lastComma = raw.lastIndexOf(',');
      final lastDot = raw.lastIndexOf('.');
      if (lastComma > lastDot) {
        // 1.234,56 → Türk formatı
        final cleaned = raw.replaceAll('.', '').replaceAll(',', '.');
        return double.tryParse(cleaned);
      } else {
        // 1,234.56 → İngiliz formatı
        final cleaned = raw.replaceAll(',', '');
        return double.tryParse(cleaned);
      }
    } else if (raw.contains(',')) {
      // 2400,00 → virgül ondalık
      return double.tryParse(raw.replaceAll(',', '.'));
    } else {
      return double.tryParse(raw);
    }
  }

  static String? extractDate(String text) {
    final patterns = [
      RegExp(r'(\d{2})[./\-](\d{2})[./\-](\d{4})'),
      RegExp(r'(\d{4})[./\-](\d{2})[./\-](\d{2})'),
    ];
    for (final pattern in patterns) {
      final match = pattern.firstMatch(text);
      if (match != null) {
        if (match.group(3)!.length == 4) {
          return '${match.group(3)}-${match.group(2)}-${match.group(1)}';
        } else {
          return '${match.group(1)}-${match.group(2)}-${match.group(3)}';
        }
      }
    }
    return null;
  }

  static String? extractStoreName(String text) {
    final skipKeywords = RegExp(
      r'^\d|fiş|fatura|tarih|saat|kasa|kasiyer|tel|vergi|tc|no:|:\s|\*|www|http|kdv|toplam|tutar|ödeme|kredi|nakit|puan|pos|işlem|işyeri|afiyet|nüsha|ziraat|isbank|visa|kart|aid|onay',
      caseSensitive: false,
    );

    final lines = text
        .split('\n')
        .map((l) => l.trim())
        .where((l) => l.isNotEmpty && l.length > 2)
        .toList();

    if (lines.isEmpty) return null;

    for (final line in lines.take(8)) {
      if (skipKeywords.hasMatch(line)) continue;
      final letterCount = line.replaceAll(RegExp(r'[^a-zA-ZğüşıöçĞÜŞİÖÇ\s]'), '').trim().length;
      if (letterCount >= 3) return line.toUpperCase();
    }

    return lines.first.toUpperCase();
  }
}
