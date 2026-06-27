class OcrParser {
  // Tutarı çıkar (örn. "TOPLAM 125,90 TL" → 125.90)
  static double? extractAmount(String text) {
    // Önce TOPLAM, TUTAR, GENEL TOPLAM gibi anahtar kelimelerin yanındaki sayıyı ara
    final keywordPatterns = [
      RegExp(r'(?:TOPLAM|TUTAR|GENEL TOPLAM|ÖDENECEK|TOTAL)[^\d]*(\d{1,6}[.,]\d{2})', caseSensitive: false),
      RegExp(r'(\d{1,6}[.,]\d{2})\s*TL', caseSensitive: false),
      RegExp(r'(\d{1,6}[.,]\d{2})'),
    ];

    for (final pattern in keywordPatterns) {
      final match = pattern.firstMatch(text);
      if (match != null) {
        final raw = match.group(1)!.replaceAll('.', '').replaceAll(',', '.');
        final amount = double.tryParse(raw);
        if (amount != null && amount > 0) return amount;
      }
    }
    return null;
  }

  // Tarihi çıkar (örn. "23.06.2026" veya "23/06/2026")
  static String? extractDate(String text) {
    final patterns = [
      RegExp(r'(\d{2})[./](\d{2})[./](\d{4})'),
      RegExp(r'(\d{4})[./](\d{2})[./](\d{2})'),
    ];

    for (final pattern in patterns) {
      final match = pattern.firstMatch(text);
      if (match != null) {
        if (match.group(3)!.length == 4) {
          // gg.aa.yyyy formatı → yyyy-aa-gg
          return '${match.group(3)}-${match.group(2)}-${match.group(1)}';
        } else {
          // yyyy.aa.gg formatı → yyyy-aa-gg
          return '${match.group(1)}-${match.group(2)}-${match.group(3)}';
        }
      }
    }
    return null;
  }

  // Mağaza adını çıkar (genelde ilk satır)
  static String? extractStoreName(String text) {
    final lines = text
        .split('\n')
        .map((l) => l.trim())
        .where((l) => l.isNotEmpty && l.length > 2)
        .toList();

    if (lines.isEmpty) return null;

    // İlk anlamlı satırı al (sayı veya özel karakter ağırlıklı değilse)
    for (final line in lines.take(5)) {
      final letterCount = line.replaceAll(RegExp(r'[^a-zA-ZğüşıöçĞÜŞİÖÇ]'), '').length;
      if (letterCount >= 3) return line;
    }
    return lines.first;
  }
}