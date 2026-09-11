| ID | Başlık | Öncelik | Tahmin | Durum | Sprint |
|---|---|---|---|---|---|
| SEC-005 | Bilinen CVE'li 2 bağımlılığı yükselt: `org.postgresql:postgresql` 42.7.11→42.7.12+ (CVE-2026-54291, HIGH — SCRAM channel-binding downgrade) ve `org.apache.poi:poi-ooxml` 5.2.5→5.4.0+ (GHSA-gmg8-593g-7mv3, LOW — OOXML zip duplicate-entry parsing) | Yüksek — postgresql CVE'si HIGH severity, OSV.dev'den doğrulandı (2026-09-10 taraması) | S | Draft | - |
| SEC-006 | Log çıktılarında kullanıcı e-postası (PII) düz metin loglanıyor — `WeeklySummaryScheduler.java:59`, `EmailService.java:94` (`log.error` içinde `user.getEmail()`/`toEmail`) | Orta — şifre/token değil ama PII, KVKK açısından log saklama süresiyle ilişkili bir risk | S | Draft | - |
| PERF-003 | (opsiyonel, ADR-002'de önerildi) Receipt create'e idempotency-key eklemek — sadece false-positive şikayetleri gerçekleşirse değerlendirilecek | Düşük | M | Draft | - |
