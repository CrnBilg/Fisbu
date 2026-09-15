**Durum**: Done
**Öncelik**: Yüksek
**Tahmin**: L (ADR + refactor + test)

## Kaynak
Sprint 2 backend denetimi, bulgu Y1: `AuthService.java:23-27,45-49,277-288` — `CategoryRepository`/`ReceiptRepository`/`BudgetRepository`/`SavingsGoalRepository` doğrudan enjekte edilmiş. Hesap silme akışı, bu modüllerin kendi (hexagonal) domain kurallarını bypass ediyor.

## Neden önemli
"God Service" deseni: AuthService, kimlik doğrulama sorumluluğunun çok ötesinde 4 farklı domain'in veri erişimini elinde tutuyor. Her domain kendi hexagonal port'unu tanımlamışken (`receipt`, `category`, `budget` modülleri), AuthService bunları atlayıp repository'lere doğrudan erişerek domain sınırlarını kırıyor — ARCH-001'de düzeltilen receipt↔budget döngüsel bağımlılığıyla aynı kök sorunun bir başka görünümü.

## Yaklaşım
Kod değiştirmeden önce **Architect'e danışılacak** — iki aday çözüm var, mimari karar gerektiriyor:
1. Her domain kendi "delete-user-data" port'unu (`out` port) tanımlar, AuthService bunları orkestre eder (mevcut domain event deseniyle -ARCH-001'deki `ReceiptRecordedEvent` gibi- tutarlı).
2. Hesap silme sorumluluğu AuthService'ten çıkarılıp ayrı bir `AccountDeletionService`/use-case'e taşınır, her domain'in kendi port'unu enjekte eder (God Service'i büyütmek yerine sorumluluğu ayırmak).
Karar **ADR olarak** `.sdlc/adr/`'a yazılacak.

## Kabul Kriterleri
- Mevcut hesap-silme davranışı (hangi verinin silindiği/silinmediği) ÖNCE bir regresyon testiyle kilitlenir (kod değişmeden önce).
- ADR yazılıp karar gerekçelendirilir.
- AuthService artık 4 repository'yi doğrudan enjekte etmiyor; her domain kendi port'u üzerinden erişiliyor.
- Regresyon testi (öncesi/sonrası) aynı sonucu doğrular — davranış değişmez, sadece mimari sınır düzelir.

## Çıktı / Doğrulama
- **ADR**: `.sdlc/adr/ADR-004-authservice-god-service.md` (Architect) — Seçenek 2 seçildi: her domain kendi "delete-user-data" out-port'unu tanımlar, yeni bir `AccountDeletionService` bunları FK sırasına göre (budget → savingsGoal → receipt → category → user) tek transaction'da doğrudan/sıralı çağırır. Event-driven (ADR-001 deseni) reddedildi çünkü bu iş akışı sıralamaya bağımlı/atomik olmalı, event bu garantiyi örtük hale getirirdi.
- **Yeni port'lar**: `receipt/application/port/out/DeleteReceiptsByUserPort`, `category/application/port/out/DeleteCategoriesByUserPort`, `budget/application/port/out/DeleteBudgetsByUserPort`, `shared/application/port/out/DeleteSavingsGoalsByUserPort` (SavingsGoal henüz hexagonal'a taşınmadığı için geçici olarak `shared`'da — ADR'de not edildi, ayrı bir takip story'si önerildi).
- **Yeni adapter**: `shared/adapter/out/persistence/SavingsGoalDeletionAdapter` (legacy `SavingsGoalRepository`'yi sarmalıyor). Diğer 3 port, mevcut `ReceiptPersistenceAdapter`/`CategoryPersistenceAdapter`/`BudgetPersistenceAdapter`'a eklendi (yeni adapter sınıfı gerekmedi).
- **Yeni sınıf**: `service/AccountDeletionService` — `@Transactional deleteAccount(String email)`, 4 port + `UserRepository` enjekte ediyor, mevcut silme mantığını (`repository.deleteAll(repository.findByUser(user))`) birebir port çağrılarına taşıdı.
- **AuthService değişikliği**: `deleteAccount()` metodu ve `CategoryRepository` HARİÇ (hâlâ `createDefaultCategories`'te kullanılıyor) `ReceiptRepository`/`BudgetRepository`/`SavingsGoalRepository` alanları + constructor parametreleri kaldırıldı. `AuthService` artık sadece kimlik doğrulama/kayıt/profil sorumluluğunu taşıyor.
- **AuthController değişikliği**: `DELETE /auth/account` artık `AccountDeletionService.deleteAccount(...)`'ı çağırıyor (path/davranış değişmedi).
- **Regresyon testi**: Mevcut davranışı kilitleyen test ZATEN VARDI (SEC-003'te yazılmış `AuthServiceTest.deleteAccount_*` — 404 senaryosu + FK sıralı silme senaryosu). Bu iki test `AccountDeletionServiceTest`'e taşındı ve port mock'larıyla yeniden yazıldı — ayrıca `InOrder` ile TAM sıralamayı (budget→savingsGoal→receipt→category→user) doğrulayacak şekilde GÜÇLENDİRİLDİ (orijinal test sadece çağrı sayılarını doğruluyordu, sıralamayı değil).
- **Doğrulama**: `./gradlew compileJava compileTestJava` başarılı. `./gradlew test --tests AuthServiceTest --tests AccountDeletionServiceTest --tests AuthControllerTest --rerun` → 23+2+11 = 36/36 test geçti, 0 hata. `./gradlew e2eTest --rerun` → BUILD SUCCESSFUL (gerçek `DELETE /auth/account` akışı E2E'de kapsanmıyor ama register→login→category→budget→receipt zinciri hâlâ bozulmadı).

## Öğrenilen Dersler
- Bu story'de "önce regresyon testi yaz" adımı fiilen GEREKMEDİ çünkü SEC-003'te (Sprint 1) zaten yazılmıştı — geçmişte "kod değiştirmeden önce mevcut davranışı kilitle" diye yazılan testler, aylar sonra farklı bir refactor'da (ARCH-002) hazır bir güvenlik ağı olarak geri ödemesini yaptı. Bu, failure-first/regresyon-önce disiplininin faydasının hemen değil, İLERİDE bir refactor anında ortaya çıktığının somut bir örneği.
- Testi taşırken sadece "aynı assertion'ları koru" değil, "bu refactor'un asıl garantisi neydi" diye sorup testi güçlendirmek (`verify(times())` → `InOrder`) ucuza geldi ve gelecekte sıralama yanlışlıkla bozulursa (örn. biri port çağrı sırasını değiştirirse) daha kesin bir hata verecek.
- AuthService'in `CategoryRepository`'yi TAMAMEN kaldırmak yerine kısmen tutması (çünkü `createDefaultCategories`'te hâlâ kullanılıyor, kayıt akışının bir parçası) bilinçli bir sınır — ADR'nin kapsamı SADECE `deleteAccount()`'taki 4-repository sorununu hedefliyordu; "kayıt sırasında varsayılan kategori oluşturma" ayrı bir konu (kendi ownership'i AuthService'te, o akışın parçası) ve bu ARCH story'sinin kapsamı dışında bırakıldı — kapsamı gerektiğinden fazla büyütmemek bilinçli bir tercih.
