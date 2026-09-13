**Durum**: Done
**Tahmin**: S

## Kaynak
PERF-004'te kanıtlanan kök nedenin düzeltmesi: `PushNotificationService.init()` (bildirim izni isteği) `main()`'de `runApp()`'dan ÖNCE senkron `await` ediliyordu, native izin diyaloğu kapatılana kadar hiçbir Flutter UI render edilmiyordu.

## Write-set
- `mobile/lib/main.dart`

## Kabul Kriterleri
İlk frame, bildirim izni diyaloğunun sonucunu beklemeden render edilir. Mevcut davranış (izin isteniyor olması, token backend'e kaydediliyor olması) korunur — sadece TETİKLEME ZAMANI değişir.

## Çıktı
`main.dart`: `await PushNotificationService.init();` bloklayan zincirden çıkarıldı. `runApp(MyApp(...))` çağrıldıktan SONRA, `WidgetsBinding.instance.addPostFrameCallback` ile (ilk frame çizildikten hemen sonra) arka planda tetikleniyor. Hata sessizce yutulmuyor (`FlutterError.reportError` ile raporlanıyor) ama uygulamanın açılışını artık bloklamıyor.

## Doğrulama — canlı emulator'de önce/sonra ölçümü
- **Önce (PERF-004, izin verilmemiş)**: "Uygulama açılışı" adımı **33+ saniye** boyunca hiçbir Flutter widget'ı göstermedi (native izin diyaloğu bloklamıştı).
- **Sonra (PERF-005 uygulandıktan sonra, izin YİNE ÖNCEDEN VERİLMEDEN — en zorlu senaryo)**: "Uygulama açılışı" adımı **5.41-5.46 saniyeye** düştü ve Login ekranı (`Tekrar\nHoşgeldin`) başarıyla bulundu — test bir sonraki adıma (email/şifre girme, login) geçti. İki ayrı çalıştırmada tutarlı sonuç (5413ms, 5456ms).
- `flutter analyze lib/main.dart` — sorun yok.
- Genel karar: **Geçti.** Puan: **9/10** (Kod kalitesi: 9 — minimal, odaklı değişiklik, mevcut davranış korunuyor; Performans: 10 — 33+s'den 5.4s'ye, izin durumundan bağımsız; Test kapsamı: 8 — gerçek emulator'de canlı doğrulandı ama otomatik bir regresyon testi eklenmedi, mevcut E2E testi bu iyileşmeyi dolaylı kanıtlıyor) → Onay.

## İnceleme
Değişiklik cerrahi ve düşük riskli — sadece ÇAĞRILMA ZAMANI değişti, iş mantığı aynı. `registerToken()`'ın `init()`'in tamamlanmasına bağımlı olmadığı doğrulandı (race condition riski yok). **Karar: Onay.**

## Öğrenilen Dersler
- `WidgetsBinding.instance.addPostFrameCallback`, "ilk frame'i bloklamadan arka planda bir şey başlat" için Flutter'ın standart, düşük riskli deseni — `main()`'i async/await zincirinden çıkarmaktan daha az invaziv.
- Bu düzeltmenin gerçek etkisini ÖLÇEBİLMEK için E2E-001'de zaten yazılmış olan `timedStep` deseni doğrudan işe yaradı — "önce/sonra" karşılaştırması saniyeler içinde somut sayılarla kanıtlanabildi, bu da E2E test altyapısına yapılan yatırımın gerçek bir geri dönüşü oldu.