**Durum**: Done
**Tahmin**: S

## Kaynak
Sprint 2 backend denetimi, bulgu A4 (scheduler N+1 + eksik index).

## Sorun
`return_deadline`/`warranty_expiry_date` kolonlarında index yok, `WarrantyReminderScheduler` her gün bu kolonlarla tam tablo taraması yapıyor. `WeeklySummaryScheduler` her kullanıcı için ayrı receipt sorgusu çalıştırıyor (N+1).

## Write-set
- `backend/src/main/resources/db/migration/V2__*.sql` (yeni index'ler)
- `backend/src/main/java/com/fisbu/api/service/WarrantyReminderScheduler.java`
- `backend/src/main/java/com/fisbu/api/service/WeeklySummaryScheduler.java`
- `backend/src/main/java/com/fisbu/api/repository/ReceiptRepository.java` (gerekirse batch sorgu)

## Kabul Kriterleri
- `return_deadline`, `warranty_expiry_date` üzerinde index var (migration ile, canlıya HITL süreciyle uygulanacak — devops.md).
- WeeklySummaryScheduler, kullanıcı başına ayrı sorgu yerine tek/az sayıda toplu sorgu kullanır.
- Mevcut scheduler testleri (varsa) veya yeni yazılan testler regresyon göstermez.

## Çıktı
- **Yeni**: `backend/src/main/resources/db/migration/V2__scheduler_indexes.sql` — `return_deadline`/`warranty_expiry_date` üzerinde, gerçek sorgu paternine (`WHERE ... AND return_reminder_sent/warranty_reminder_sent = false`) uyan **partial index**'ler (`WHERE return_reminder_sent = false` vb.) — tam index'ten daha küçük ve daha etkili.
- **Değişti**: `ReceiptRepository.java` — `sumTotalAmountGroupedByUserForDateRange` (userId'ye göre gruplanmış tek sorgu, mevcut `sumTotalAmountByUserAndCategoryAndReceiptDateBetween` deseniyle tutarlı).
- **Değişti**: `WeeklySummaryScheduler.java` — kullanıcı başına 2 ayrı sorgu (N+1) yerine, tüm kullanıcılar için TOPLAM 2 agregasyon sorgusu (haftalık/önceki hafta), sonra bellekte `Map` lookup. Davranış AYNI (aynı bildirim mantığı), sadece sorgu sayısı kullanıcı sayısından bağımsız hale geldi.
- `WarrantyReminderScheduler.java`'da kod değişikliği YOK — sorun sadece eksik index'ti, sorgu paterni zaten tek sorguydu.
- **Yeni test**: `WeeklySummarySchedulerTest.java` — 5 test (push yapılandırılmamışsa sorgu atmama, N+1 olmadığını doğrulayan sorgu-sayısı testi, sıfır harcamada bildirim göndermeme, haritada olmayan kullanıcı için 0 varsayılan, bir kullanıcıda hata olursa diğerlerinin etkilenmemesi).

## Doğrulama
- `./gradlew compileJava` / `compileTestJava` — BUILD SUCCESSFUL.
- `./gradlew test --tests "com.fisbu.api.service.WeeklySummarySchedulerTest"` — BUILD SUCCESSFUL, JUnit XML: `tests="5" failures="0" errors="0"`.
- **Canlı doğrulama (2026-09-11): BAŞARILI.** Kullanıcı `./gradlew bootRun` çalıştırdı — PERF-001'in V3'üyle birlikte tek turda: `Successfully validated 4 migrations`, `Current version: 3`, `Started ApiApplication in 7.227 seconds`, hiç hata yok. `idx_receipts_return_deadline` ve `idx_receipts_warranty_expiry_date` partial index'leri canlıda oluşturuldu.
- Genel karar: **Geçti.** Puan: **8/10** (Kod kalitesi: 8 — N+1 doğru şekilde tek agregasyon sorgusuna indirgendi; Test kapsamı: 8 — scheduler'ın ilk testi, davranış korunduğu doğrulandı; Performans: 8 — sorgu sayısı O(kullanıcı) → O(1)) → Onay.

## İnceleme
Kod değişikliği küçük ve odaklı (tek repository metodu + scheduler'ın sorgulama şekli). Davranış regresyonu yok — testler bunu doğruluyor. Canlı ortamda index'ler başarıyla oluşturuldu. **Karar: Onay.**

## Öğrenilen Dersler
- `ReceiptRepository`'de zaten `sumTotalAmountByUserAndCategoryAndReceiptDateBetween` gibi bir DB-taraflı agregasyon örneği olması, N+1 düzeltmesini şablonlaştırılmış/düşük riskli hale getirdi — sıfırdan bir desen icat etmek gerekmedi.
- `List.of(new Object[]{...})` (tek elemanlı) Java'da varargs belirsizliğine yol açıyor (derleyici `Object[]`'i varargs dizisi olarak açıyor) — `Collections.singletonList(...)` kullanmak gerekti. Test kodunda küçük ama gerçek bir Java tuzağı.
- `WarrantyReminderScheduler`'ın koduna hiç dokunmadan (sadece index ekleyerek) sorunu çözebilmek, "önce ölç/oku, sonra düzelt" yaklaşımının değerini gösterdi — ilk bakışta "scheduler'da N+1 var" gibi görünse de, o scheduler'ın asıl sorunu kod değil index eksikliğiydi.
