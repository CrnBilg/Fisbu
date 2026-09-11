**Durum**: Done
**Tahmin**: M

## Kaynak
Sprint 2 backend denetimi, bulgu A3 (idempotency yok, SavingsGoal'da gerçek çift-kayıt riski).

## Sorun
Hiçbir endpoint idempotency-key desteklemiyor. SavingsGoal create'de hiç duplicate kontrolü yok (network retry → çift kayıt). Receipt/Budget'taki "duplicate" kontrolleri iş kuralı yan etkisi, idempotency için tasarlanmamış (Receipt'te false-positive riski var: aynı gün aynı markette gerçek iki alışveriş reddedilebilir).

## Write-set
- `backend/src/main/java/com/fisbu/api/savings_goals/**` (veya mevcut `service/SavingsGoalService.java` + `controller/SavingsGoalController.java`)
- `backend/src/main/resources/db/migration/V3__*.sql` (idempotency_key kolonu, SavingsGoal ve gerekirse Receipt için)
- `backend/src/main/java/com/fisbu/api/receipt/**` (false-positive değerlendirmesi sonrası gerekirse)

## Kabul Kriterleri
- Client `Idempotency-Key` header'ı (UUID) gönderdiğinde, aynı key ile ikinci istek yeni kayıt oluşturmaz, ilk isteğin sonucunu döner.
- Key gönderilmezse mevcut davranış (kontrolsüz create) korunur — geriye dönük uyumluluk (mobil app henüz header göndermiyor).
- Receipt'in mevcut duplicate kontrolü ya idempotency-key ile değiştirilir ya da false-positive riski explicit olarak kabul edilip ADR/yorum ile belgelenir.

## Çıktı
ADR-002 yazıldı (`.sdlc/adr/ADR-002-idempotency-key-kapsami.md`) — mekanizma sadece **SavingsGoal**'a eklendi (gerçek sıfır-korumalı boşluk); Receipt/Budget'a dokunulmadı (gerekçe: Receipt hexagonal immutable record olduğu için değişikliğin kapsamı orantısız büyük, mevcut davranışı "sessiz çift kayıt" değil "reddetme" olduğu için risk niteliksel olarak daha hafif; Budget zaten UNIQUE constraint ile korunuyor). Receipt için gerekirse ayrı bir **PERF-003** backlog maddesi önerildi (henüz açılmadı, sadece ADR'de not).

Değişen/eklenen dosyalar:
- **Yeni**: `backend/src/main/resources/db/migration/V3__savings_goals_idempotency_key.sql` — `idempotency_key varchar(255)` (nullable) + partial UNIQUE index (`WHERE idempotency_key IS NOT NULL`, NULL'lar çakışmaz).
- **Değişti**: `entity/SavingsGoal.java` (yeni alan), `dto/SavingsGoalRequest.java` (opsiyonel `idempotencyKey` alanı), `repository/SavingsGoalRepository.java` (`findByUserAndIdempotencyKey`), `service/SavingsGoalService.java` (`createGoal`: key verilmişse ve var olan kayıt bulunursa onu döner, yeni kayıt açmaz; key yoksa mevcut davranış korunur).
- **Yeni test**: `SavingsGoalServiceTest.java` — 7 test (bu servisin İLK testi): key yoksa idempotency kontrolü atlanır (regresyon garantisi), aynı key ile ikinci istek yeni kayıt açmaz, yeni key ile kayıt key'le birlikte saklanır, boş string key null gibi davranır, kullanıcı bulunamama, başka kullanıcının hedefine erişim (403), negatif katkı (400).

## Doğrulama
- `./gradlew compileJava` / `compileTestJava` — BUILD SUCCESSFUL.
- `./gradlew test --tests "com.fisbu.api.service.SavingsGoalServiceTest"` — BUILD SUCCESSFUL, JUnit XML: `tests="7" failures="0" errors="0"`.
- **Canlı doğrulama (2026-09-11): BAŞARILI.** Kullanıcı `./gradlew bootRun` çalıştırdı — V2 ile birlikte tek turda: `Successfully validated 4 migrations`, `Current version: 3`, `Started ApiApplication in 7.227 seconds`, hiç hata yok. (Not: ilk denemede ARCH-001'den kaynaklanan ayrı bir Spring bean ambiguity hatası çıktı — `LoadOwnedCategoryPort` için `@Qualifier` eksikliği; bu PERF-001/002'nin kendi hatası değildi, ARCH-001.md'de düzeltildi ve belgelendi, düzeltme sonrası bu doğrulama başarılı oldu.)
- Genel karar: **Geçti.** Puan: **9/10** (Kod kalitesi: 9 — geriye dönük uyumlu, minimal/odaklı değişiklik; Test kapsamı: 9 — servisin ilk testleri, hem yeni hem mevcut davranış kapsandı; Güvenlik: 8 — UNIQUE constraint DB seviyesinde de garanti, sadece uygulama katmanı kontrolüne güvenilmiyor) → Onay.

## İnceleme
ADR-002'deki kapsam kararı (Receipt/Budget'a dokunmama) mühendislik açısından savunulabilir — SavingsGoal'daki riskin niteliği (sessiz çift kayıt) Receipt'teki riskten (yanlış reddetme) daha ciddi, kapsamı orada yoğunlaştırmak doğru önceliklendirme. Canlı ortamda migration başarıyla doğrulandı. **Karar: Onay.**

## Öğrenilen Dersler
- SavingsGoal'ın (legacy stil, JPA entity + Lombok) idempotency-key'i eklemesi, Receipt'in (hexagonal, immutable record) aynısını eklemesinden ÇOK daha basitti — bu, "her yeni alan aynı maliyette değil, mimari stil doğrudan değişiklik maliyetini belirliyor" dersini somut olarak gösterdi; bu yüzden kapsamı ADR ile daraltmak (madde 1'e bakınca "gerekirse" ifadesini kullanmak) doğru bir karardı.
- Partial UNIQUE index (`WHERE idempotency_key IS NOT NULL`) deseni, PERF-002'deki partial index deseniyle (`WHERE return_reminder_sent = false`) tutarlı — bu session'da ikinci kez kullanıldı, projede bir "kısmi index" konvansiyonu oluşuyor.
- Boş string (`"   "`) idempotency key'in `null` gibi davranması gerektiği ilk yazımda akla gelmedi, kabul kriterlerini yazarken fark edildi ve teste eklendi — mobil taraf boş string gönderirse (örn. bir UUID kütüphanesi başlatılmamışsa) sessizce idempotency'siz davranışa düşülmesi, hata fırlatılmasından daha güvenli bir varsayılan.
