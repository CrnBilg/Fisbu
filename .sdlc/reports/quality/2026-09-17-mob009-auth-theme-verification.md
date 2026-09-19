# Faz 5 Doğrulama Raporu — Auth Ekranlarında Sabit Koyu Tema

**Tarih**: 2026-09-17
**Kapsam**: 2026-09-17 UI/UX denetim raporunun A4 bulgusu ("Auth akışının 5 ekranı tema sistemini tamamen atlıyor") — dosya:satır kanıtı eksikti, bu tur SADECE doğrulama için açıldı. **Kod değiştirilmedi.**

## Sonuç: DOĞRULANDI — yanlış pozitif değil

5 ekranın tamamında, arka plan `Scaffold.body`'nin kökünde sabit bir `LinearGradient` — `[Color(0xFF0F0F1A), Color(0xFF1A1A2E)]` — olarak tanımlı. Hiçbirinde `Theme.of(context).brightness`, `isDark`, `ThemeController` veya `MyApp.of(context)` referansı YOK (her dosyada ayrı ayrı `grep -c` ile doğrulandı, hepsi **0** sonuç döndü). Yani bu gradient, sistem/uygulama teması ne olursa olsun her zaman aynı şekilde render ediliyor.

| Dosya | Satır | Kanıt |
|---|---|---|
| `lib/screens/login_screen.dart` | 82-86 | `Container(decoration: const BoxDecoration(gradient: LinearGradient(... colors: [Color(0xFF0F0F1A), Color(0xFF1A1A2E)]))` — `Scaffold.body`'nin (satır 78) ilk çocuğu, tam ekran arka plan |
| `lib/screens/register_screen.dart` | 202-208 | Aynı desen, `Scaffold.body`'de (satır 203) doğrudan `Container` üzerinde |
| `lib/screens/register_screen.dart` | 30 | Bonus: KVKK bottom-sheet'i de ayrıca `backgroundColor: const Color(0xFF1A1A2E)` ile sabit |
| `lib/screens/forgot_password_screen.dart` | 65-70 | Aynı desen |
| `lib/screens/reset_password_screen.dart` | 130-135 | Aynı desen |
| `lib/screens/verify_email_screen.dart` | 107-112 | Aynı desen |

## Ek bulgu (doğrulama sırasında ortaya çıktı): kullanılan renkler `AppColors`'ın kendi koyu palet değerleriyle de UYUŞMUYOR

`lib/core/theme/app_colors.dart:30-31`:
```dart
static const Color backgroundDark = Color(0xFF0F172A);  // Slate-900
static const Color surfaceDark = Color(0xFF1E293B);      // Slate-800
```
Auth ekranlarındaki sabit değerler (`0xFF0F0F1A`, `0xFF1A1A2E`) bunlardan FARKLI — yani bu ekranlar sadece "tema sistemini atlamıyor", aynı zamanda uygulamanın geri kalanının kullandığı koyu palet değerlerinden de sapan, elle seçilmiş ayrı bir renk kümesi kullanıyor. Bu, A4 bulgusunu daha da güçlendiriyor: sorun sadece "ışık/koyu geçişi yok" değil, "bu 5 ekranın koyu hali bile uygulamanın geri kalanıyla piksel-eşleşmiyor".

## Kapsam dışı bırakılanlar (bu turda dokunulmadı)
`pin_entry_screen.dart` ve `auth_wrapper.dart` (aynı auth akışının parçası ama orijinal 5'li listede değildi) zaten doğru şekilde `AppColors.txt(context)`/`surf(context)` kullanıyor — bunlar sorunun DIŞINDA, referans/örnek olarak kalabilir.

## Sonraki adım
Bu bulgu artık dosya:satır kanıtıyla belgelenmiş durumda. Düzeltme (5 ekranın `AppColors`/`ThemeController`'a bağlanması) HENÜZ YAPILMADI — kullanıcı onayı bekleniyor.
