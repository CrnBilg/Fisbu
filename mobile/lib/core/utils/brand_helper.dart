/// Mağaza adından, elle küratörlüğü yapılmış bir logo eşleşmesi bulur.
/// CategoryHelper ile aynı desen: basit, offline, eşleşme yoksa null döner
/// (çağıran taraf CategoryHelper ikonuna düşer). Logo PNG dosyaları
/// mobile/assets/logos/ altına eklendikçe bu liste büyütülür — dosya henüz
/// eklenmemiş bir marka burada olsa bile BrandAvatar widget'ı hatasız şekilde
/// kategori ikonuna düşer (bkz. core/widgets/brand_avatar.dart).
class BrandHelper {
  BrandHelper._();

  static const Map<String, String> _logos = {
    'ebebek': 'assets/logos/ebebek.png',
    'a101': 'assets/logos/a101.png',
    'bim': 'assets/logos/bim.png',
    'sok': 'assets/logos/sok.png',
    'migros': 'assets/logos/migros.png',
    'carrefour': 'assets/logos/carrefoursa.png',
    'teknosa': 'assets/logos/teknosa.png',
    'mediamarkt': 'assets/logos/mediamarkt.png',
    'lc waikiki': 'assets/logos/lcwaikiki.png',
    'koton': 'assets/logos/koton.png',
    'defacto': 'assets/logos/defacto.png',
    'zara': 'assets/logos/zara.png',
    'shell': 'assets/logos/shell.png',
    'opet': 'assets/logos/opet.png',
    'petrol ofisi': 'assets/logos/petrolofisi.png',
    'yemeksepeti': 'assets/logos/yemeksepeti.png',
    'getir': 'assets/logos/getir.png',
    'trendyol': 'assets/logos/trendyol.png',
    'ikea': 'assets/logos/ikea.png',
    'starbucks': 'assets/logos/starbucks.png',
  };

  static String? getLogoAsset(String? storeName) {
    if (storeName == null || storeName.isEmpty) return null;
    final normalized = _normalize(storeName);
    for (final entry in _logos.entries) {
      if (normalized.contains(entry.key)) return entry.value;
    }
    return null;
  }

  static String _normalize(String s) => s
      .toLowerCase()
      .replaceAll('ı', 'i')
      .replaceAll('ş', 's')
      .replaceAll('ğ', 'g')
      .replaceAll('ü', 'u')
      .replaceAll('ö', 'o')
      .replaceAll('ç', 'c');
}
