# ADR-001: Receipt↔Budget Döngüsel Bağımlılığının Çözümü

**Durum**: Kabul edildi
**Tarih**: 2026-09-10
**İlgili story**: ARCH-001
**Kaynak**: Sprint 2 backend denetimi, bulgu A1

## Bağlam
`receipt` modülü, bir fiş oluşturulduğunda bütçe eşiğini kontrol etmek için `budget`'ın in-port'unu (`CheckBudgetThresholdUseCase`) doğrudan çağırıyor. `budget` modülü ise eşik hesaplamak için `receipt`'in out-port'unu (`SumReceiptSpendByCategoryPort`) çağırıyor. Sonuç: `receipt → budget` ve `budget → receipt` aynı anda var — döngüsel bağımlılık. Ayrıca `LoadOwnedCategoryPort` arayüzü her iki modülde birebir aynı içerikle iki kez tanımlanmış.

Fonksiyonel ihtiyaç gerçek: fiş eklendiğinde bütçe eşiği kontrol edilmeli (receipt→budget yönü), ve eşik hesabı için o ayki/kategorideki toplam fiş harcaması bilinmeli (budget→receipt yönü). İkisi de gerekli — sorun "ihtiyaç" değil, "senkron doğrudan çağrı" ile modellenmiş olması.

## Değerlendirilen Seçenekler

### Seçenek 1: Budget'ın kendi repository'si üzerinden receipt tablosunu doğrudan sorgulaması
Artı: Tek yönlü bağımlılık (budget→receipt kalkar, budget kendi verisini kendi sorgular).
Eksi: Receipt'in hexagonal encapsulation'ını bypass eder — tam olarak Sprint 1/2 denetimlerinde eleştirdiğimiz "legacy servislerin entity'ye doğrudan erişimi" sorununu bu sefer budget'a taşır. Reddedildi.

### Seçenek 2: Domain event ile receipt→budget çağrısını tersine çevirmek (seçildi)
Receipt, bir fiş kaydedildiğinde `ReceiptRecordedEvent` (userId, categoryId, year, month) yayınlar (Spring'in `ApplicationEventPublisher`'ı ile — ekstra bir mesaj kuyruğu altyapısı gerektirmez, aynı süreç içinde senkron/eşzamanlı dinleyici). `budget` modülü bu event'i dinleyen bir `@EventListener` ile eşik kontrolünü kendisi tetikler.
Artı: `receipt` artık `budget`'ın hiçbir port'unu import ETMEZ — döngü tek yönde kalır (`budget → receipt`, sadece `SumReceiptSpendByCategoryPort` üzerinden, ki bu zaten mecburi ve doğal: eşik hesabı için harcama toplamı gerekiyor). Domain event deseni, hexagonal/DDD'de "bir aggregate'in başka bir aggregate'i doğrudan çağırmaması" prensibiyle uyumlu.
Eksi: Event yayınlama asenkron algısı yaratabilir ama burada SENKRON dinleyici kullanılacak (aynı transaction/thread içinde, `@TransactionalEventListener` DEĞİL, düz `@EventListener`) — davranış (kullanıcı fiş ekler, aynı istekte push bildirimi tetiklenir) korunur, sadece çağrı yönü tersine döner.

### Seçenek 3: İki modülü birleştirmek
Reddedildi — kapsam gereğinden büyük, iki farklı bounded context'i (finansal kayıt vs. bütçe planlama) gereksiz yere birleştirir.

## Karar
**Seçenek 2** uygulanır:
1. `receipt` modülüne `ReceiptRecordedEvent` (basit bir record/POJO, `shared` pakette değil, `receipt/domain/event/` altında — receipt'in kendi domain event'i) tanımlanır.
2. `ReceiptService.createReceipt()`, `checkBudgetThresholdUseCase.checkAndNotify(...)` çağrısı yerine `applicationEventPublisher.publishEvent(new ReceiptRecordedEvent(...))` çağırır. `CheckBudgetThresholdUseCase` import'u receipt'ten kaldırılır.
3. `budget` modülünde yeni bir `@Component` (`budget/adapter/in/event/ReceiptRecordedEventListener`) eklenir, `@EventListener` ile `ReceiptRecordedEvent`'i dinler ve mevcut `CheckBudgetThresholdUseCase`'i çağırır (bu use-case'in kendisi ve iç mantığı DEĞİŞMEZ, sadece tetikleyici değişir).
4. `LoadOwnedCategoryPort`'un iki kopyası, `com.fisbu.api.shared.application.port.out.LoadOwnedCategoryPort` altında BİRLEŞTİRİLİR; hem `budget` hem `receipt` bu ortak arayüzü kullanır, adapter implementasyonları (Category modülüne delege eden) aynı kalır, sadece import değişir.

## Sonuç
Bağımlılık grafiği: `receipt → shared` (LoadOwnedCategoryPort için), `budget → receipt` (SumReceiptSpendByCategoryPort için, tek yön), `budget → shared`. `receipt`, `budget`'ın HİÇBİR port'una artık bağımlı değil. Davranış değişmez (aynı istekte eşik kontrolü hâlâ tetiklenir), sadece çağrı mekanizması event-driven'a döner.
