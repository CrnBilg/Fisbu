# ADR-002: Idempotency-Key Mekanizmasının Kapsamı

**Durum**: Kabul edildi
**Tarih**: 2026-09-11
**İlgili story**: PERF-001
**Kaynak**: Sprint 2 backend denetimi, bulgu A3

## Bağlam
Denetim, 3 create endpoint'ini inceledi: SavingsGoal (hiç duplicate kontrolü yok — gerçek çift-kayıt riski), Receipt (store+amount+date bazlı duplicate kontrolü var ama false-positive riski taşıyor — aynı gün aynı markette gerçek iki alışveriş reddedilebilir), Budget (kategori+ay tekilliği zaten UNIQUE constraint ile garanti — retry'de ikinci istek doğal olarak `DuplicateBudgetException` alır, ek bir mekanizma gerekmez).

Soru: Idempotency-Key mekanizması sadece SavingsGoal'a mı eklenmeli, yoksa Receipt'in mevcut kontrolü de aynı mekanizmayla değiştirilmeli mi?

## Değerlendirme
Receipt, hexagonal mimariye taşınmış ve `Receipt` immutable domain record'u (15 alan) `ReceiptService` içinde 4 farklı yerde (`createReceipt`, `setReminders`, `saveSplit`, ve test yardımcılarında) inşa ediliyor. Receipt'e yeni bir `idempotencyKey` alanı eklemek, bu record'u ve onu inşa eden TÜM call site'ları (adapter/persistence/mapper dahil) değiştirmeyi gerektirir — riski/kapsamı SavingsGoal'a göre orantısız büyük.

Ayrıca Receipt'in mevcut davranışı "sessizce çift kayıt oluşturma" değil, "reddetme" (`DuplicateReceiptException`) — risk veri bütünlüğü değil, kullanıcı sürtünmesi (nadir bir kenar durumda meşru bir ikinci fiş reddedilebilir). Bu, SavingsGoal'daki "hiçbir kontrol yok, sessizce çift kayıt oluşur" riskinden niteliksel olarak daha hafif.

## Karar
Idempotency-Key mekanizması **sadece SavingsGoal'a** eklenir (gerçek, sıfır korumalı boşluk). Receipt'in mevcut duplicate kontrolü ŞİMDİLİK değiştirilmez — false-positive riski kabul edilir ve bu ADR ile belgelenir. Budget'a hiçbir değişiklik gerekmez (UNIQUE constraint zaten yeterli).

Eğer Receipt'teki false-positive riski gerçek kullanıcı şikayetlerine yol açarsa (bu ADR'nin varsayımı yanlış çıkarsa), ayrı bir backlog maddesi (PERF-003) açılıp Receipt'in domain record'una idempotency key eklenmesi o zaman değerlendirilir — şimdiden PERF-003 backlog'a not olarak düşülüyor.

## Sonuç
- SavingsGoal: `idempotency_key` kolonu + UNIQUE(user_id, idempotency_key) partial index (NULL'lar hariç — mobil app key göndermezse mevcut davranış korunur).
- Receipt/Budget: değişiklik yok, risk kabul edildi ve belgelendi.
