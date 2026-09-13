**Durum**: Done (araştırma — düzeltme YAPILMADI, kullanıcı talebiyle sadece rapor)
**Tahmin**: -

## Kaynak
E2E-001/PERF-004 araştırması sırasında tesadüfen bulunan, login adımının ~24 saniye sürdüğü gözlemi. Kullanıcı talebi: kök nedenin PERF-004 ile aynı kaynaktan mı (bildirim izni bloklaması) yoksa backend'den mi (senkron bir API çağrısı) kaynaklandığının araştırılması.

## ÖNEMLİ DÜZELTME
Bu story'nin ilk sürümü (aşağıda korunmuştur) "kök neden: `flutter_secure_storage`/Android Keystore ilk kullanım maliyeti" sonucuna varmıştı. Bu hipotez **doğrulanmadan** rapor edilmişti ve doğrulama adımı için ek testler yapıldığında **yanlış olduğu kanıtlandı**. Gerçek kök neden aşağıda "Gerçek kök neden (düzeltilmiş)" bölümündedir.

## Gerçek kök neden (düzeltilmiş)
**`AuthService.login()`'un kendisi hiçbir zaman yavaş değildi — 24 saniyelik rakam bir ölçüm artefaktıydı, gerçek bir kullanıcı gecikmesi değildi.**

Doğrulama adım 1 — `AuthService.login()`'u izole ölçüm (`perf006_second_login_test.dart`, aynı kurulumda 3 kez arka arkaya login/logout):
```
İlk login (taze kurulum, Keystore alias'ı ilk kez oluşturuluyor): 581 ms
İkinci login (aynı kurulum): 99 ms
Üçüncü login (aynı kurulum): 79 ms
```
Bu, önceki sürümün "Keystore ilk kullanım maliyeti 24 saniye sürüyor" iddiasını **doğrudan çürütür** — ilk çağrı bile sadece 581ms.

Doğrulama adım 2 — gerçek UI akışında ("Giriş Yap" tıklamasından Dashboard'ın gerçekten göründüğü ana kadar), `pumpAndSettle(Duration(seconds: 8))` yerine 500ms'lik ince taneli `pump()` döngüsüyle ölçüm (`perf006_ui_timing_test.dart`):
```
Giriş Yap tıklamasından Dashboard (Fiş Ekle metni) görünene kadar: 1223 ms
Dashboard göründüğü anda CircularProgressIndicator hâlâ dönüyor mu: false
```
**Gerçek kullanıcı deneyimi: ~1.2 saniye.** 24 saniyelik rakam yoktu.

### Artefaktın mekanizması
`receipt_to_budget_flow_test.dart`'taki orijinal ölçüm şekli:
```dart
await tester.tap(find.text('Giriş Yap'));
await tester.pumpAndSettle(const Duration(seconds: 8));
```
`pumpAndSettle(duration)`, widget ağacında yeni frame zamanlanmayana kadar `duration` uzunluğunda adımlarla tekrar tekrar pump eder — `duration` bir üst zaman sınırı DEĞİL, her tekrarın adım büyüklüğüdür. Dashboard'da (ilk render sırasında kısa süreliğine) bir `CircularProgressIndicator` (belirsiz/sürekli animasyonlu spinner, `lib/screens/dashboard_screen.dart:173`) döndüğü için, Flutter test binding'i "hâlâ yeni frame zamanlanıyor" görüp `pumpAndSettle`'ı bir sonraki 8 saniyelik pencereye geçirdi. Üç ayrı çalıştırmada gecikmenin ~24.3s'de (yani tam olarak 3×8s) sabitlenmesi, bunun rastgele bir gecikme değil, **tam olarak 3 pumpAndSettle iterasyonunun** tetiklendiğinin kanıtıdır — spinner'ın kendisi 1.2 saniyeden fazla sürmedi, ama `pumpAndSettle`'ın "hâlâ animasyon var" tespiti onu üçüncü 8 saniyelik pencereye kadar bekletti.

### Ekarte edilenler (önceki sürümden korunmuştur, hâlâ geçerli)
1. **Backend değil**: Backend E2E testinde (`RegisterToBudgetFlowE2ETest`) `POST /auth/login` adımı **97ms** sürdü.
2. **Ağ/emulator gecikmesi değil**: `ping` (<2ms), ham `nc` isteği (**0.03s**).
3. **PERF-004 ile aynı kaynak değil**: PERF-005 sonrası bu gecikme aynı şekilde devam etti (o zamanki ölçüm şekliyle).
4. **(Yeni) Keystore/secure-storage ilk kullanım maliyeti değil**: doğrudan ölçüldü, en kötü durumda 581ms.

## Sonuç
**Gerçek bir performans sorunu YOK.** "24 saniyelik login gecikmesi" gözlemi, `pumpAndSettle`'ın indeterminate bir spinner'a duyarlılığından kaynaklanan bir test-ölçüm artefaktıydı. Kullanıcının bildirdiği "kasma/donma" şikayetinin gerçek kaynağı PERF-004'tü (bildirim izni diyaloğunun `runApp()`'ı bloklaması) ve PERF-005 ile zaten düzeltildi. Login akışında ek bir düzeltmeye gerek yoktur.

## Aksiyon: PERF-007 backlog maddesi
PERF-007 ("secure-storage yazma işlemini navigasyonu bloklamadan yapmak") bu düzeltilmiş bulgu ışığında **kapatılmalı** — dayandığı öncül ("login her zaman/ilk kullanımda yavaş") artık geçersiz. Aşağıda not edilmiştir; backlog-done.md'ye "Kapatıldı — gerçek bir sorun değildi" olarak taşınacaktır.

## Yazılan/değiştirilen dosyalar
- **Hiçbir production kodu değiştirilmedi.**
- Tanı amaçlı testler: `mobile/integration_test/perf006_second_login_test.dart`, `mobile/integration_test/perf006_ui_timing_test.dart` (geçici teşhis testleri, kalıcı test paketine dahil edilmedi).

## Öğrenilen Dersler
- **Bir hipotezi "doğrulanmadan" rapor etmek riskliydi.** İlk sürümde Keystore hipotezi mantıklı görünüyordu (literatürde bilinen bir davranış) ama gerçek ölçüm yapılmadan sunulmuştu. Kullanıcı "kök nedeni araştır" dediğinde, kök neden koddan MANTIKEN çıkarılabilir görünse de, **doğrudan ölçülmeden kesin gibi rapor edilmemeli**.
- `pumpAndSettle(Duration)`'ın `duration` parametresi bir timeout DEĞİL, iterasyon adım büyüklüğüdür — indeterminate bir animasyon (spinner, shimmer, vb.) varken bu parametre çok büyük seçilirse (8s gibi), gerçekte 1 saniyeden kısa süren bir işlem, ölçümde dakikalarca uzun görünebilir. E2E testlerinde zamanlama iddiaları için `pumpAndSettle` DEĞİL, ince taneli (`~100-500ms`) bir `pump()` döngüsü + açık bir hedef-widget kontrolü kullanılmalı.
- Üç bağımsız kanıt katmanı (backend E2E zamanlaması, ham ağ testi, kod okuma) "yanlış" bir dördüncü hipotezi (Keystore) çürütmedi çünkü hiçbiri DOĞRUDAN `AuthService.login()`'u veya gerçek UI zamanlamasını izole ölçmüyordu — sonunda gereken şey, iddiayı DOĞRUDAN test eden iki yeni tanı testiydi.
