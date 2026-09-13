# Mobil (Flutter) Denetim Raporu — App Store Hazırlığı

**Tarih**: 2026-09-13
**Kapsam**: `mobile/` — mimari, performans, App Store hazırlığı, test kapsamı, statik analiz
**Yöntem**: Salt-okunur kod incelemesi + `flutter analyze`/`flutter test` gerçek çalıştırma + dosya sistemi taraması. Kod değişikliği yapılmadı.

---

## ACİL

### A1 — `PrivacyInfo.xcprivacy` dosyası yok
**Kanıt**: `find . -iname "*PrivacyInfo*"` → sonuç boş (`mobile/ios/Runner/` altında yok).
**Neden kritik**: Apple, Mayıs 2024'ten itibaren üçüncü taraf SDK'ları (Firebase, flutter_secure_storage, vb. "required reason API" kullanan kütüphaneler) içeren tüm App Store submission'larında Privacy Manifest dosyasını **zorunlu** kılıyor. Bu proje Firebase Core/Messaging/Crashlytics ve flutter_secure_storage (Keystore/Keychain — required-reason API) kullanıyor. Dosya eksikse **App Store Connect submission derleme/inceleme aşamasında reddedilebilir**.
**Öneri**: `ios/Runner/PrivacyInfo.xcprivacy` oluşturulmalı; ayrıca kullanılan paketlerin (`firebase_*`, `flutter_secure_storage`, `local_auth`) kendi privacy manifest'lerini içerip içermediği (CocoaPods üzerinden otomatik birleşiyor mu) doğrulanmalı.

### A2 — Sıfır çalışan otomatik test, `flutter test` derleme hatasıyla başarısız
**Kanıt**: `mobile/test/widget_test.dart:3,7` — varsayılan Flutter şablonu hiç değiştirilmemiş (`import 'package:mobile/main.dart';` — gerçek paket adı `fisbu`; `MyApp(initialDarkMode: false)` çağrısı gerçek constructor imzasıyla uyuşmuyor). `flutter test` çalıştırıldığında **0 test geçiyor, 1 derleme hatası** veriyor.
**Neden kritik**: ~16.000 satırlık `lib/` koduna karşılık gerçek anlamda **hiçbir unit/widget testi yok** ve mevcut tek dosya bile çalışmıyor. `integration_test/` altındaki 3 dosya (E2E-001/PERF-006 kapsamında bu oturumda eklendi) cihaz/emulator gerektiriyor, normal CI'da/`flutter test`'te çalışmaz. App Store'a çıkmadan önce projede regresyona karşı sıfır otomatik koruma var.

### A3 — Servis katmanında sessizce yutulan hatalar (boş `catch`)
**Kanıt**:
- `lib/services/auth_service.dart:57` — `catch (e) {}` (logout'ta token silme hatası tamamen yutuluyor).
- `lib/services/auth_service.dart:113` — `getProfile()`'da `catch (e) {}`, sessizce `null` dönüyor.
- `flutter analyze` bunları `empty_catches` lint uyarısı olarak zaten işaretliyor.
**Neden kritik**: Hata ne loglanıyor ne kullanıcıya bildiriliyor — production'da neyin bozulduğunu asla göremezsiniz (Crashlytics'e bile düşmüyor). Güvenlik-ilgili bir işlemde (token silme) sessiz başarısızlık özellikle riskli.

---

## YÜKSEK

### Y1 — Dashboard'da hata durumu kullanıcıya hiç gösterilmiyor
**Kanıt**: `lib/screens/dashboard_screen.dart:39-56` (`_loadReceipts`) — fiş/bütçe çekme hatası `catch (e) { setState(() => _isLoading = false); }` ile yutuluyor, log yok, kullanıcı mesajı yok. Sonuç ekranı boş listeye (`_receipts=[]`) düşüyor — gerçek bir ağ hatası kullanıcıya "hiç fişin yok" olarak görünüyor.
**Neden önemli**: Kullanıcı deneyimi olarak yanıltıcı; App Store review'da da "veri kaybı gibi görünen" davranış olumsuz not alabilir.

### Y2 — Dashboard'da bağımsız network çağrıları paralelleştirilmemiş
**Kanıt**: aynı `_loadReceipts()` içinde `ReceiptService.getReceipts()` ve `BudgetService.getBudgets()` birbirine bağımlı olmadığı halde sıralı `await`'leniyor (`Future.wait` yerine). Karşılaştırma: `lib/screens/statistics_screen.dart:69-73` 5 yükleme fonksiyonunu paralel (fire-and-forget, her biri kendi `setState`'i ile) çalıştırıyor — proje içinde iki farklı, tutarsız desen var.
**Neden önemli**: PERF-004/PERF-006 emsaliyle aynı kategori — küçük ama gerçek, kümülatif açılış gecikmesi; ayrıca tutarsız desen bakım riskini artırıyor.

---

## ORTA

### O1 — Merkezi state management yok, `setState` her yerde (208 çağrı / 27 dosya)
**Kanıt**: `pubspec.yaml`'da provider/riverpod/bloc/get_it yok. 27 ekran dosyasında toplam 208 `setState()` çağrısı; sadece tema için elle yazılmış bir `ValueNotifier`/`InheritedNotifier` (`ThemeController`, `main.dart:112-119`) var.
**Neden önemli**: Proje ölçeğinde (69 dart dosyası, ~16K satır, 27 ekran) bu, her ekranda aynı loading/error/setState iskeletinin kopyalanıp yapıştırılmasına yol açıyor (dashboard/statistics/receipt_list hepsinde tekrarlanan try/catch+setState deseni) — Y1'in kök nedeni de kısmen bu (merkezi bir hata-durumu deseni olmadığı için her ekran kendi başına, tutarsız şekilde hata yönetiyor).

### O2 — `flutter analyze`: 115 gerçek proje sorunu
**Kanıt**: `flutter analyze lib test` → 115 issue (2 error = A2'deki bozuk şablon, kalanı `info`): ağırlıklı `deprecated_member_use` (`withOpacity`→`withValues`, örn. `register_screen.dart`'ta 12+ kullanım) ve `use_build_context_synchronously` (`receipt_list_screen.dart:398,408`).
**Not**: argümansız `flutter analyze` çalıştırılırsa `build/ios/.../firebase_messaging/test` gibi paket-içi otomatik üretilmiş dosyalardan gelen sahte hatalar toplamı 271'e şişiriyor; gerçek proje kapsamı (`lib`+`test`) 115. CI eşiği ayarlanırken bu ayrım netleştirilmeli.

---

## DÜŞÜK

### D1 — Klasör organizasyonu düzenli (olumlu bulgu)
`core/theme`, `core/utils`, `core/widgets`, `models/`, `screens/`, `services/` ayrımı tutarlı biçimde uygulanmış — mimari düzensizlik bulgusu yok.

### D2 — Test/kod oranı fiilen %0
`lib/` 15.963 satır, `test/` altında tek (bozuk) dosya. Servis katmanı (`auth_service.dart`, `receipt_service.dart` vb.) için hiç unit test yok.

---

## App Store Hazırlık Kontrol Listesi — Özet Durum

| Kontrol | Durum | Kanıt |
|---|---|---|
| İzin açıklama metinleri (kamera/mikrofon/FaceID/galeri) | ✅ Yeterli | `ios/Runner/Info.plist` — 4 açıklama, Türkçe, spesifik |
| Privacy Manifest (`PrivacyInfo.xcprivacy`) | ❌ **Eksik (ACİL — A1)** | dosya bulunamadı |
| Crashlytics aktif/yapılandırılmış | ✅ Doğrulandı | native config dosyaları mevcut, gradle plugin'leri ekli, debug'da kapalı/release'de açık (`main.dart:44`), PII maskeleme (`_sanitizeForCrashlytics`) var |
| Hesap silme akışı (Apple zorunlu kılıyor) | ✅ Erişilebilir | `profile_screen.dart:938` → `_handleDeleteAccount()` → `AuthService.deleteAccount()` → `DELETE /auth/account`; Dashboard→Profile navigasyonu doğrulandı (`dashboard_screen.dart:136`) |

---

## Özet Öncelik Sıralaması
1. **A1** — PrivacyInfo.xcprivacy ekle (submission-blocker olabilir)
2. **A2** — widget_test.dart'ı düzelt + gerçek bir minimum unit/widget test seti kur
3. **A3** — boş `catch` bloklarını (auth_service.dart:57,113) logla/işle
4. **Y1** — Dashboard hata durumunu kullanıcıya göster
5. **Y2** — Dashboard'daki sıralı await'leri `Future.wait` ile paralelleştir
6. **O1/O2** — orta vadede: state management standardizasyonu, deprecated API temizliği
