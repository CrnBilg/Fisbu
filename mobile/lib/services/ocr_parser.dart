class OcrParser {
  // Tutarı çıkar
  static double? extractAmount(String text) {
    final lines = text.split('\n');

    // Önce TOPLAM/TUTAR içeren satırlarda ara
    final keywordPatterns = [
      RegExp(r'(?:GENEL TOPLAM|ÖDENECEK TUTAR|ÖDENECEK|TOPLAM TUTAR|TOPLAM|TUTAR|TOTAL)[^\d]*\*?(\d{1,6}[.,]\d{2})', caseSensitive: false),
    ];

    for (final line in lines) {
      for (final pattern in keywordPatterns) {
        final match = pattern.firstMatch(line);
        if (match != null) {
          final raw = match.group(1)!.replaceAll('.', '').replaceAll(',', '.');
          final amount = double.tryParse(raw);
          if (amount != null && amount > 0 && amount < 100000) return amount;
        }
      }
    }

    // KDV, TOPKDV gibi satırları atla, TL ile biten sayıları ara
    final tlPattern = RegExp(r'\*?(\d{1,6}[.,]\d{2})\s*TL', caseSensitive: false);
    double? largest;
    for (final line in lines) {
      final lower = line.toLowerCase();
      if (lower.contains('kdv') && !lower.contains('toplam')) continue;
      if (lower.contains('indirim')) continue;
      final match = tlPattern.firstMatch(line);
      if (match != null) {
        final raw = match.group(1)!.replaceAll('.', '').replaceAll(',', '.');
        final amount = double.tryParse(raw);
        if (amount != null && amount > 0 && amount < 100000) {
          if (largest == null || amount > largest) largest = amount;
        }
      }
    }
    if (largest != null) return largest;

    // Son çare: en büyük sayıyı al
    final anyPattern = RegExp(r'\*?(\d{1,6}[.,]\d{2})');
    double? fallback;
    for (final line in lines) {
      final match = anyPattern.firstMatch(line);
      if (match != null) {
        final raw = match.group(1)!.replaceAll('.', '').replaceAll(',', '.');
        final amount = double.tryParse(raw);
        if (amount != null && amount > 0 && amount < 100000) {
          if (fallback == null || amount > fallback) fallback = amount;
        }
      }
    }
    return fallback;
  }

  // Tarihi çıkar
  static String? extractDate(String text) {
    final patterns = [
      RegExp(r'(\d{2})[./](\d{2})[./](\d{4})'),
      RegExp(r'(\d{4})[./](\d{2})[./](\d{2})'),
      RegExp(r'(\d{2})-(\d{2})-(\d{4})'),
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

  // Mağaza adını çıkar
  static String? extractStoreName(String text) {
    final skipKeywords = RegExp(
      r'^\d|fiş|fatura|tarih|saat|kasa|kasiyer|tel|vergi|tc|no:|:|\*|www|http|kdv|toplam|tutar|ödeme|kredi|nakit|puan',
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
