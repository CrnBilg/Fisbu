| ID | Başlık | Öncelik | Tahmin | Durum | Sprint |
|---|---|---|---|---|---|
| SEC-001 | Eski pakette (Household, SavingsGoal, Budget, Auth/User controller'ları) her `{id}` alan endpoint için ownership/IDOR kontrolünü doğrula | Acil — güvenlik denetimi bulgusu, kod değişikliği gerektirmeyen bir tarama/rapor işi | S | Done | 1 |
| SEC-003 | `AuthService.java` için ownership/regresyon testleri yaz (kod değiştirmeden önce mevcut davranışı kilitle) | Yüksek — güvenlik denetimi bulgusu | S | Done | 1 |
| SEC-004 | Deploy gate'inin (CI yeşil olmadan deploy engelleniyor mu) Railway/GitHub ayarlarından doğrulanması | Yüksek — güvenlik denetimi bulgusu, repo dışı bir ayar; agent'lar tespit edip raporlar, kendileri değiştirmez | S | Done | 1 |
| SEC-002 | Migration disiplini kurulumu — Flyway ekle, mevcut şemayı baseline migration olarak snapshot'la, `ddl-auto`'yu `validate`'e çevir | Acil — güvenlik denetimi bulgusu, migration disiplini eksikliği veri tutarlılığı riski taşıyor | M | Done | 1 |
| ARCH-001 | Receipt↔Budget döngüsel bağımlılığı + LoadOwnedCategoryPort tekrarı düzeltmesi | Acil — Sprint 2 denetim bulgusu A1 | M | Done | 2 |
| TEST-001 | StatisticsService + ReceiptAiService regresyon testleri | Acil — Sprint 2 denetim bulgusu A5 | M | Done | 2 |
| TEST-002 | StatementImportService regresyon testleri | Acil — Sprint 2 denetim bulgusu A5 | S | Done | 2 |
| TEST-003 | Auth/Budget/Household controller testleri (3/10, kalan TEST-004'te) | Acil — Sprint 2 denetim bulgusu A5 | L | Done | 2 |
| PERF-002 | Scheduler index eksikliği (return_deadline/warranty_expiry_date) + N+1 düzeltmesi | Acil — Sprint 2 denetim bulgusu A4 | S | Done | 2 |
| PERF-001 | Idempotency-Key mekanizması (SavingsGoal) | Acil — Sprint 2 denetim bulgusu A3 | M | Done | 2 |
| TEST-004 | Kalan 7 controller için test yaz (SavingsGoal/StatementImport/Statistics/Upload/Inflation/AI/User) | Acil — Sprint 2 denetim bulgusu A5 | M | Done | 2 |
| SEC-005 | Bilinen CVE'li 2 bağımlılığı yükselt (postgresql, poi-ooxml) | Yüksek | S | Done | - |
| SEC-006 | Log çıktılarında kullanıcı e-postası (PII) sızıntısı düzeltmesi | Orta | S | Done | - |
| E2E-001 | Backend (Testcontainers) + mobil (integration_test) E2E stratejisi, örnek testler, agent kuralları | Yüksek — kullanıcı talebi, gerçek bir production kasma bulgusu ortaya çıkardı | L | Done | - |
| PERF-004 | "Debug modda kasma/donma" araştırması — kök neden: main()'in bildirim izni diyaloğunu runApp()'dan önce bloklaması | Yüksek — gerçek kullanıcı şikayeti, kod satırı seviyesinde kanıtlandı | - | Done (araştırma) | - |
| PERF-005 | PERF-004'ün düzeltmesi — bildirim izni artık runApp()'dan sonra tetikleniyor, açılış 33+s'den 5.4s'ye düştü (canlı emulator'de kanıtlandı) | Yüksek | S | Done | - |
| PERF-006 | ~24s login gecikmesi araştırması — DÜZELTİLDİ: gerçek kök neden `pumpAndSettle`'ın indeterminate spinner'a duyarlılığından kaynaklanan ölçüm artefaktı; gerçek gecikme ~1.2s, gerçek bir performans sorunu yok | Orta | - | Done (araştırma, düzeltilmiş sonuç) | - |
| PERF-007 | (PERF-006'nın düzeltilmiş sonucu nedeniyle Kapatıldı — dayandığı öncül geçersiz, secure-storage yazma işlemi hiçbir zaman yavaş değildi) | - | - | Kapatıldı (gerçek sorun değil) | - |
