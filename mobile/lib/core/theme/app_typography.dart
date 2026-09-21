import 'package:flutter/material.dart';

/// Proje genelindeki adlandırılmış tipografi ölçeği. Öncesinde ekranlar
/// font boyutlarını serbest sayı olarak (14/15/16/18/13/20/22/24/17/30/36/40
/// gibi birbirine yakın ama tutarsız değerlerle) yazıyordu — bu sınıf o
/// dağınık kullanımı adlandırılmış, anlamlı bir ölçeğe bağlar.
///
/// Renk BURADA sabitlenmez (context-duyarlı AppColors çağrıları çoğu yerde
/// gerekiyor) — her stil `copyWith(color: ...)` ile kullanılır.
class AppTypography {
  AppTypography._();

  /// 40 — Auth ekranlarının büyük başlığı ("Tekrar Hoşgeldin" vb.)
  static const TextStyle display = TextStyle(
    fontSize: 40,
    fontWeight: FontWeight.w800,
    height: 1.1,
    letterSpacing: -1.0,
  );

  /// 36 — Dashboard/receipt_detail'deki büyük tutar gösterimi
  static const TextStyle amountLarge = TextStyle(
    fontSize: 36,
    fontWeight: FontWeight.w800,
    letterSpacing: -1,
  );

  /// 24 — Ekran içi büyük vurgu metni (ör. persona başlığı)
  static const TextStyle headlineLarge = TextStyle(
    fontSize: 24,
    fontWeight: FontWeight.w800,
  );

  /// 22 — Kart/bölüm başlıkları (SliverAppBar başlığı, aile bakiyesi vb.)
  static const TextStyle headline = TextStyle(
    fontSize: 22,
    fontWeight: FontWeight.w800,
  );

  /// 20 — İkincil ekran/adım başlıkları (doğrulama adımı başlığı vb.)
  static const TextStyle title = TextStyle(
    fontSize: 20,
    fontWeight: FontWeight.w800,
  );

  /// 18 — Bölüm başlığı (AppBar title, "Henüz X yok" boş durum başlığı)
  static const TextStyle sectionTitle = TextStyle(
    fontSize: 18,
    fontWeight: FontWeight.w700,
  );

  /// 16 — Alt bölüm/kart başlığı
  static const TextStyle cardTitle = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.w700,
  );

  /// 15 — Standart gövde metni (form alanı değeri, liste satırı başlığı)
  static const TextStyle body = TextStyle(
    fontSize: 15,
    fontWeight: FontWeight.w500,
  );

  /// 14 — İkincil gövde metni / buton etiketi
  static const TextStyle bodySecondary = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.w500,
  );

  /// 13 — Etiket/altyazı (subtitle, tarih, yardımcı açıklama)
  static const TextStyle label = TextStyle(
    fontSize: 13,
    fontWeight: FontWeight.w500,
  );

  /// 12 — Küçük altyazı (chip, meta bilgi)
  static const TextStyle caption = TextStyle(
    fontSize: 12,
    fontWeight: FontWeight.w500,
  );

  /// 11 — En küçük etiket (grafik ekseni, çok yoğun listeler)
  static const TextStyle micro = TextStyle(
    fontSize: 11,
    fontWeight: FontWeight.w500,
  );
}
