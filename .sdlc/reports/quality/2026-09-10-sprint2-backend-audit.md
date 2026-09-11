# Sprint 2 — Backend Uçtan Uca Denetim Raporu

**Tarih**: 2026-09-10
**Kapsam**: Mimari, performans, test kapsamı, kod kalitesi (statik analiz), API tasarımı/sözleşmesi, loglama/gözlemlenebilirlik, i18n kapsamı.
**Yöntem**: 5 paralel salt-okunur denetim + 1 dosya değişikliği (Checkstyle eklendi ve çalıştırıldı — kullanıcı tarafından açıkça istendi). Hiçbir uygulama kodu değiştirilmedi.
**Not**: Bu rapor bir keşif turudur, düzeltme fazı değildir. Her bulgu gerçek dosya okunarak/gerçek komut çalıştırılarak doğrulanmıştır, varsayımla üretilmemiştir.

---

## ACİL

### A1. `receipt` ↔ `budget` modülleri arasında döngüsel bağımlılık
- `receipt/application/service/ReceiptService.java:16` → `budget.application.port.in.CheckBudgetThresholdUseCase` import ediyor.
- `budget/adapter/out/receipt/BudgetReceiptSpendAdapter.java:8` → `receipt.application.port.out.SumReceiptSpendByCategoryPort` import ediyor.
- İki modül birbirine hem "in" hem "out" port üzerinden bağımlı — hexagonal'ın tek yönlü bağımlılık hedefi kırılmış. Ayrıca `LoadOwnedCategoryPort` arayüzü hem `budget/application/port/out/` hem `receipt/application/port/out/` altında AYRI AYRI tanımlanmış (aynı kavramın iki kopyası).

### A2. API versiyonlama hiç yok — mobil kırılma riski
- Hiçbir controller'da `/api/v1` gibi bir prefix yok (`/auth`, `/receipts`, `/budgets` düz path). Mobil taraf (`api_client.dart:13,32`) da versiyon segmenti kullanmıyor.
- Somut senaryo: backend'de bir response alanı breaking şekilde değişirse (örn. `categoryId` → `category` objesi), TÜM mobil client'lar (mağazada onay sürecinde olan eski sürümler dahil) anında etkilenir — geriye dönük destek/rollback imkanı yok.

### A3. Idempotency mekanizması hiçbir endpoint'te yok — SavingsGoal'da gerçek çift-kayıt riski
- `grep -rn "Idempotency"` → 0 sonuç, hiçbir yerde `Idempotency-Key` header desteği yok.
- **SavingsGoal create** (`SavingsGoalService.java:39-49`): HİÇBİR duplicate kontrolü yok — network retry'de aynı hedef iki kez oluşur.
- Receipt/Budget'ta "duplicate" kontrolleri var ama bunlar İDEMPOTENCY için TASARLANMAMIŞ, iş kuralı yan etkisi: Receipt'te store+amount+date eşleşmesi false-positive riski taşıyor (kullanıcı aynı gün aynı markette gerçekten iki kez alışveriş yaparsa reddedilir); Budget'ta kategori/ay tekilliği tesadüfen koruma sağlıyor.

### A4. Scheduler'lar N+1 + index'siz full table scan yapıyor
- `WeeklySummaryScheduler.java:52,78` — tüm FCM-token'lı kullanıcıları çekip her biri için ayrı receipt sorgusu (N+1).
- `WarrantyReminderScheduler.java:46,64` — `return_deadline`/`warranty_expiry_date` filtreli sorgular, **bu kolonlarda hiç index yok** (`V1__baseline.sql`'de teyit edildi) — receipts tablosu büyüdükçe her gün tam tablo taraması.

### A5. Finansal veri işleyen 3 servis (ve tüm controller'lar) hiç test edilmiyor
- `StatisticsService` (393 satır), `ReceiptAiService` (325 satır), `StatementImportService` (229 satır) — sıfır test. Tüm 10 controller sınıfı da (HTTP giriş noktası + yetkilendirme burada) test edilmemiş.

### A6. `AiService` senkron/bloklayan, rate-limit veya circuit breaker yok
- `AiService.java:91,95` — 30 sn'ye kadar bloklayan senkron HTTP çağrısı, Groq 429 durumunda retry/backoff yok. Eşzamanlı AI kullanımı Tomcat worker thread havuzunu doldurup normal trafiği de yavaşlatabilir.

---

## YÜKSEK

### Y1. `AuthService` "God Service" — 4 farklı domain'in repository'sine doğrudan erişiyor
`AuthService.java:23-27,45-49,277-288` — `CategoryRepository`/`ReceiptRepository`/`BudgetRepository`/`SavingsGoalRepository` doğrudan enjekte edilmiş; hesap silme, bu modüllerin kendi (hexagonal) domain kurallarını bypass ediyor.

### Y2. `HouseholdService`/`StatisticsService` receipt'in hexagonal port'larını atlayıp entity'ye doğrudan erişiyor
`HouseholdService.java:27-29,117-143`, `StatisticsService.java:27-35,74,105-122` — `ReceiptRepository`/`ReceiptItemRepository` doğrudan kullanılıyor. Receipt domain modeli değişirse bu iki servis kırılır.

### Y3. `StatementImportService` dosyayı tamamen belleğe okuyor
`StatementImportService.java:186,195-201` — PDF/CSV tüm dosya byte/String olarak belleğe çekiliyor, `MAX_AI_TEXT_LENGTH=8000` sadece AI'ya giden metni sınırlıyor, okuma/parse aşamasını değil. Büyük ekstre yüklemesi bellek riski taşır.

### Y4. `StatisticsService`/`PersonalInflationService` sınırsız veri çekip Java'da agregasyon yapıyor
`StatisticsService.java:139,203,279` ve `PersonalInflationService.java:71` — `findByUser`/`findByReceipt_User_Id` sınırsız, `ReceiptService`'teki `MAX_RECEIPTS_LIST=2000` deseni burada YOK. Uzun süreli aktif kullanıcılarda (binlerce fiş/kalem) gecikme birikir.

### Y5. `UploadController` yükleme sahipliğini hiç kaydetmiyor
`UploadController.java:36-38` — `userDetails` parametre olarak alınıyor ama KULLANILMIYOR; Cloudinary'ye sabit klasöre yükleniyor. Audit/quota/abuse-tracking için hangi kullanıcının ne yüklediği hiçbir yerde yok.

### Y6. Hata yanıtı `details` alanının semantiği modüller arası tutarsız/dokümante değil
`GlobalExceptionHandler.java:44-52` — `details` map'i SADECE Bean Validation hatalarında dolduruluyor; domain kuralı ihlallerinde (`DuplicateBudgetException` vb.) hep `null`. İstemci bu ayrımı zımni öğrenmek zorunda.

### Y7. Pagination'sız repository/port metodları (liste)
`ReceiptRepository.findByUser/findByCategory`, `ReceiptItemRepository` (2 metod), `BudgetRepository` (3 metod), `SavingsGoalRepository.findByUser`, `CategoryRepository.findByUser`, `LoadBudgetsPort` (2 metod), `LoadCategoriesPort.loadByUserId`, `FindReceiptsByStoreNameContainingPort`, `FindDuplicateReceiptPort` — hiçbiri limit/Pageable almıyor (`LoadReceiptsPort.loadByUserId` iyi örnek, limit alıyor).

### Y8. Test edilmemiş yetkilendirme-riskli servisler + `NeedBraces` gerçek bulgusu
`UserService`, `PersonalInflationService`, `FinancialChatService`, `AiService` test edilmemiş. Ayrıca Checkstyle gerçek çalıştırmasında **auth-hassas kod yolunda parantezsiz `if`** bulundu: `AuthService.java:96-97`, `ReceiptAiService.java:115-118` — bir sonraki satırın yanlışlıkla if kapsamına girmesi/çıkması riski taşıyan gerçek bir kod kalitesi sinyali (gürültü değil).

---

## ORTA

### O1. Ownership-check kod tekrarı — 7 serviste birebir aynı `getUserByEmail` helper'ı
`HouseholdService:185-188`, `ReceiptAiService:321-324`, `SavingsGoalService:165-168`, `UserService:66-69`, `StatisticsService:388-392`, `StatementImportService:225-228`, `PersonalInflationService:147-151` — ortak bir `UserLookupService` yok.

### O2. MapStruct legacy modüllerde hiç kullanılmıyor
Sadece `category`/`receipt`/`budget` adapter'larında `@Mapper` var; legacy servislerde tüm DTO mapping elle (constructor/setter zinciri).

### O3. Correlation ID / MDC mekanizması yok
`grep -rln "MDC"` → 0 sonuç. Eşzamanlı isteklerin log'ları ayırt edilemiyor — production hata ayıklaması zorlaşır.

### O4. ERROR log seviyesi, beklenen/retry-edilebilir hatalar için de kullanılıyor
`WarrantyReminderScheduler.java:58,76`, `AiService.java:106`, `ReceiptAiService.java:282` (AI format hatası — `raw` içerik PII riski de taşıyabilir) — gerçek sistem hataları (`GlobalExceptionHandler.java:84`) bu gürültüde kaybolabilir.

### O5. Eksik index adayları
`users.fcm_token` (haftalık scheduler'da kullanılıyor, tablo küçükken önemsiz), `receipt_items(receipt_id, normalized_name)` composite (PersonalInflationService'te join+filtre+sort), `users.household_id` FK'sinde index yok.

### O6. i18n kapsamı: küçük değil ama yönetilebilir büyüklükte
Backend: 48+ hardcoded TR mesaj, 9 dosya (+ custom exception çağrı noktaları, tahmini +20-30). Mobile: 30 dosyada `Text()` widget'ı, tahmini 100-300 hardcoded string. **Büyüklük mertebesi: ~40-50 dosya, yüzlerce satır** — küçük bir refactor değil, sistematik bir i18n kurulumu (`MessageSource`/`intl`) gerektirir.

### O7. `BudgetController` ve `StatementImportController`/`Service` hibrit durumda
`BudgetController` hexagonal port kullanıyor ama hâlâ legacy pakette (taşıma riski düşük, sadece paket değişikliği). StatementImport kısmen hexagonal, kısmen legacy (iş mantığı domain kuralı değil, taşımanın getirisi düşük).

### O8. Karmaşık metodlar (bölünmeye uygun)
`StatisticsService.computeSubscriptionCandidates` (~68 satır), `computePersona` (~70 satır, 6 dallanma + magic number'lar).

### O9. Checkstyle eklendi, çalıştırıldı, sonuç raporlandı (bu denetimin kendi çıktısı)
`build.gradle`'a Checkstyle (Google config, `ignoreFailures=true`, CI'yi kırmıyor) eklendi. **5321 bulgu**, ama büyük kısmı (indentation 3416, import order 684) proje stiliyle Google config uyuşmazlığından kaynaklanan gürültü. Gerçek sinyal: 8 `NeedBraces` (bkz. Y8/A-üstü), 1 `MissingSwitchDefault` (muhtemelen exhaustive enum, düşük risk), 2 `MethodName`.

---

## DÜŞÜK

- **D1**: `LoadOwnedCategoryPort` arayüzünün budget/receipt modüllerinde ayrı ayrı tanımlanması (bkz. A1) — birleştirme fırsatı.
- **D2**: `EmailService.java:68` — config eksikken WARN seviyesinde email loglanıyor (SEC-006'nın küçük bir varyantı).
- **D3**: `AiService`/`PersonalInflationService`/`FinancialChatService` hexagonal'a taşınmamış ama stateless oldukları için gerçek maliyet düşük.
- **D4**: Ölü kod YOK — `grep -rn "TODO\|FIXME\|@Deprecated"` sıfır sonuç, temiz.
- **D5**: Mobile tarafında `print()`/`debugPrint()` PII sızıntısı YOK (1 çağrı, hassas veri içermiyor) — temiz.
- **D6**: `BudgetService.java:159` switch'te default yok — muhtemelen exhaustive enum, false-positive'e yakın.

---

## Sprint 3'e not (bu turda derinlemesine girilmedi)
Flutter mobil tarafında state management yaklaşımı, erişilebilirlik, performans (gereksiz rebuild, bundle boyutu) — ayrı bir sprint'te ele alınacak, bu denetimde sadece işaretlendi.

---

## Denetim metodolojisi notu
5 paralel agent, her biri kendi alanında gerçek dosya okuyarak/gerçek komut (`grep`, `./gradlew checkstyleMain`, `./gradlew compileJava`) çalıştırarak kanıt üretti. Hiçbir agent canlı veritabanına bağlanmadı veya `./gradlew build`/`test`/`bootRun` gibi full-context komutlarını çalıştırmadı (bilinçli kısıt, `ApiApplicationTests`'in gerçek Supabase'e bağlanmasını önlemek için).
