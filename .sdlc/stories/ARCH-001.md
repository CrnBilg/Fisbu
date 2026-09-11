**Durum**: Done
**Tahmin**: M
**İlgili ADR**: ADR-001 (bu story kapsamında yazılacak)

## Kaynak
Sprint 2 backend denetimi (`.sdlc/reports/quality/2026-09-10-sprint2-backend-audit.md`), bulgu A1.

## Sorun
`receipt` ve `budget` hexagonal modülleri birbirine hem "in" hem "out" port üzerinden bağımlı (döngüsel): `ReceiptService` → `CheckBudgetThresholdUseCase` (budget'ın in-port'u), `BudgetReceiptSpendAdapter` → `SumReceiptSpendByCategoryPort` (receipt'in out-port'u). Ayrıca `LoadOwnedCategoryPort` her iki modülde ayrı ayrı tanımlanmış (kod tekrarı + kavramsal tutarsızlık).

## Write-set
- `backend/src/main/java/com/fisbu/api/receipt/application/port/**`
- `backend/src/main/java/com/fisbu/api/budget/application/port/**`
- `backend/src/main/java/com/fisbu/api/shared/**` (gerekirse yeni ortak port/paket)
- `.sdlc/adr/ADR-001-*.md`

## Kabul Kriterleri
- Bağımlılık grafiği tek yönlü hale gelir (architect'in ADR'de belirleyeceği yön).
- `LoadOwnedCategoryPort`'un iki kopyası tek bir tanıma indirilir.
- Mevcut testler (ReceiptServiceTest, BudgetServiceTest, CategoryServiceTest) hiçbir davranış regresyonu olmadan geçer.

## Çıktı
ADR-001 yazıldı (`.sdlc/adr/ADR-001-receipt-budget-dongusel-bagimlilik.md`) — Seçenek 2 (domain event ile receipt→budget çağrısının tersine çevrilmesi) kabul edildi.

Değişen/eklenen dosyalar:
- **Silindi**: `receipt/application/port/out/LoadOwnedCategoryPort.java`, `budget/application/port/out/LoadOwnedCategoryPort.java` (iki birebir kopya).
- **Yeni**: `shared/application/port/out/LoadOwnedCategoryPort.java` (birleşik port), `receipt/domain/event/ReceiptRecordedEvent.java` (yeni domain event), `budget/adapter/in/event/ReceiptRecordedEventListener.java` (budget'ın event'i dinleyip `CheckBudgetThresholdUseCase`'i tetiklemesi).
- **Değişti**: `ReceiptCategoryAdapter.java`, `BudgetCategoryAdapter.java` (shared port'a import değişikliği), `BudgetService.java` (aynı), `ReceiptService.java` (`CheckBudgetThresholdUseCase` import/alan/constructor/çağrısı kaldırıldı, `ApplicationEventPublisher` ile event yayınlamaya geçildi).
- **Test güncellemeleri**: `ReceiptServiceTest.java` (mock `CheckBudgetThresholdUseCase` → `ApplicationEventPublisher`, ilgili 2 assertion event-publish doğrulamasına çevrildi), `BudgetServiceTest.java` (sadece import düzeltmesi).

Sonuç: `receipt` modülü artık `budget`'ın HİÇBİR port'una bağımlı değil. Bağımlılık tek yönlü: `budget → receipt` (sadece `SumReceiptSpendByCategoryPort` ve yeni `ReceiptRecordedEvent` için).

## Doğrulama
- `./gradlew compileJava` ve `./gradlew compileTestJava` — BUILD SUCCESSFUL.
- `./gradlew test --tests "com.fisbu.api.receipt.application.service.ReceiptServiceTest" --tests "com.fisbu.api.service.BudgetServiceTest" --tests "com.fisbu.api.category.application.service.CategoryServiceTest"` — BUILD SUCCESSFUL.
- JUnit XML teyidi: ReceiptServiceTest `tests="18" failures="0" errors="0"`, BudgetServiceTest `tests="9" failures="0" errors="0"`, CategoryServiceTest `tests="10" failures="0" errors="0"` — hiçbir regresyon yok.
- Genel karar: **Geçti.** Puan: **9/10** (Kod kalitesi: 9 — döngü tamamen kırıldı, kod tekrarı giderildi; Test kapsamı: 9 — 3 servisin tüm testleri regresyonsuz geçti; Güvenlik: 9 — davranış değişmedi, sadece çağrı mekanizması) → Onay.

## İnceleme
PDM, ADR-001'in seçilen çözümün (event-driven decoupling) gerçekten döngüyü kırdığını (kod okuyarak: `ReceiptService.java`'da artık `budget` paketine hiçbir import yok) ve davranışın (aynı istekte eşik kontrolü tetiklenmesi) korunduğunu (senkron `@EventListener` kullanımı) doğruladı. **Karar: Onay.**

## Güncelleme (2026-09-11) — canlı doğrulamada bulunan bir bug
Kullanıcı, PERF-001/PERF-002 için `bootRun` denerken şu hatayla karşılaştı: `BudgetService`'in `LoadOwnedCategoryPort` parametresi için Spring iki aday bean buluyordu (`budgetCategoryAdapter`, `receiptCategoryAdapter`) — ikisi de artık aynı (birleştirilmiş) arayüzü implemente ediyor. Bu, port arayüzünü birleştirirken (doğru bir adımdı) her modülün KENDİ adapter implementasyonunu koruduğumuz (doğru bir tasarım — her modül kendi adapter'ına sahip olmalı) ama Spring'e hangi bean'in hangi servise gideceğini söylemeyi UNUTTUĞUMUZ için oluştu.

**Neden testlerde yakalanmadı**: Tüm unit testler (`ReceiptServiceTest`, `BudgetServiceTest`) servisleri `new ReceiptService(...)`/`new BudgetService(...)` ile ELLE inşa ediyor — Spring DI'yi hiç devreye girmiyor, bean ambiguity'si sadece gerçek `ApplicationContext` yüklendiğinde (yani `bootRun`/`@SpringBootTest`) ortaya çıkar. Bu, projenin canlı DB'ye bağlanma riski nedeniyle `ApiApplicationTests`'i bilerek çalıştırmadığımız stratejinin bir kör noktası oldu.

**Düzeltme**: `BudgetService` constructor'ında `@Qualifier("budgetCategoryAdapter")`, `ReceiptService` constructor'ında `@Qualifier("receiptCategoryAdapter")` eklendi — sadece annotation, constructor imzası/davranış değişmedi. `./gradlew compileJava` başarılı, `ReceiptServiceTest` (18/18), `BudgetServiceTest` (9/9), `CategoryServiceTest` (10/10) regresyonsuz geçti.

**Nihai canlı doğrulama (2026-09-11)**: Kullanıcı düzeltme sonrası `./gradlew bootRun`'ı tekrar denedi — bean ambiguity hatası tamamen giderildi, `Started ApiApplication in 7.227 seconds` ile başarıyla açıldı (bkz. PERF-001/PERF-002.md — aynı bootRun turunda onlar da doğrulandı).

## Öğrenilen Dersler
- **Kritik operasyonel bulgu (bu story'ye özel değil, genel bir risk)**: Bu story'yi uygularken, paralel çalışan arka plan agent'larından (TEST-001, TEST-003) en az biri süreç içinde `git stash` çalıştırdı — bu, PDM'in (benim) o an commit edilmemiş TÜM değişikliklerimi (SEC-002'nin dosya değişiklikleri dahil) working tree'den kaldırdı. İki kez oldu, ikisinde de `git stash list`/`git stash show` ile içerik teyit edilip `git stash pop` ile hiçbir veri kaybı olmadan kurtarıldı — ama bu şans eseri fark edildi (derleme hatası → "dosya beklediğim gibi değil" → git status kontrolü). Ders: birden fazla agent aynı git working tree'sinde eşzamanlı çalışırken, bir agent'ın `git stash`/`checkout`/`reset` gibi TÜM repo'yu etkileyen komutları çalıştırması, başka bir agent'ın/PDM'in commit edilmemiş işini görünmez şekilde silebilir. Öneri: (a) çakışan write-set'i olan işler gerçekten sıralı çalıştırılmalı (PDM'in paralellik kararı kuralı bunu zaten söylüyor, ama "git komutu çalıştırma" da bir write-set çakışması sayılmalı), (b) agent'lara verilen talimatlarda "git stash/checkout/reset gibi TÜM çalışma alanını etkileyen komutları çalıştırma, sadece kendi dosyalarını Read/Edit et" kısıtı eklenmeli.
- Mimari değişikliğin kendisi (döngüsel bağımlılığı kırma) teknik olarak sorunsuzdu — asıl zaman kaybı yukarıdaki git çakışmasını fark edip kurtarmaktan kaynaklandı.
- `LoadOwnedCategoryPort`'un iki kopyasının birebir aynı olması, birleştirmeyi mekanik/risksiz hale getirdi — davranış değişikliği gerektirmedi, sadece import yönü değişti.
- **Gerçek kör nokta**: iki modülün aynı arayüzü implemente eden ayrı `@Component` bean'leri, sadece gerçek Spring context'i (bootRun/`@SpringBootTest`) yüklendiğinde ambiguity hatası verir — mock-based unit testler bunu YAKALAYAMAZ çünkü DI konteynerini hiç kullanmazlar. Ders: bir arayüzü birden fazla modülde birleştirirken (aynı arayüz, ayrı implementasyonlar deseni), her zaman `@Qualifier` gerekip gerekmediği kontrol edilmeli — bu proje "canlı DB'ye bağlanan `ApiApplicationTests`'i çalıştırma" stratejisi izlediği için, bu tür DI-seviyeli hatalar ancak kullanıcının kendi `bootRun` denemesiyle yakalanabildi. Gelecekte benzer bir port birleştirmesi yapılırken bu kontrol PDM/reviewer tarafından bilerek sorulmalı: "bu arayüzün birden fazla implementasyonu var mı, varsa @Qualifier eklendi mi?"
