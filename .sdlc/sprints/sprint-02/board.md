# Sprint 2

**Sprint Hedefi**: Sprint 2 backend denetimi (`.sdlc/reports/quality/2026-09-10-sprint2-backend-audit.md`) düzeltme fazı — ACİL bulgular öncelikli.

## Story'ler

| ID | Başlık | Tahmin | Durum |
|---|---|---|---|
| ARCH-001 | Receipt↔Budget döngüsel bağımlılığı + LoadOwnedCategoryPort tekrarı | M | Done |
| PERF-001 | Idempotency-Key mekanizması (SavingsGoal öncelikli) | M | Done |
| PERF-002 | Scheduler index eksikliği + N+1 düzeltmesi | S | Done |
| TEST-001 | StatisticsService + ReceiptAiService regresyon testleri | M | Done |
| TEST-002 | StatementImportService regresyon testleri | S | Done |
| TEST-003 | Auth/Budget/Household controller testleri | L | Done (3/10 controller) |
| TEST-004 | Kalan 7 controller testi | M | Done (85 test toplam, 10/10 controller) |

## Akış
- ARCH-001: architect (ADR) → backend-dev → qa
- PERF-001/002: backend-dev/devops → qa → reviewer
- TEST-*: backend-dev/qa (regresyon, kod değiştirmeden)

## Önemli operasyonel not
ARCH-001 sırasında paralel çalışan bir test-yazım subagent'ı (TEST-003), kendi doğrulaması için `git stash`/`git stash pop` kullandı — bu, PDM'in eşzamanlı commit edilmemiş değişikliklerini iki kez geçici olarak "kaybetmiş" gibi gösterdi (her ikisinde de `git stash pop` ile sorunsuz kurtarıldı, veri kaybı olmadı). Ders: birden fazla agent aynı git working tree'sinde eşzamanlı çalışırken, tüm repo'yu etkileyen git komutları (`stash`/`checkout`/`reset`) başka bir agent'ın işini görünmez şekilde etkileyebilir. Bkz. ARCH-001.md Öğrenilen Dersler.

## Canlı doğrulama (2026-09-11)
`./gradlew bootRun` — V2+V3 migration'ları tek turda doğrulandı: `Successfully validated 4 migrations`, `Current version: 3`, `Started ApiApplication in 7.227 seconds`. İlk denemede ARCH-001'den kaynaklanan bir Spring bean ambiguity hatası çıktı (`LoadOwnedCategoryPort` için `@Qualifier` eksikliği), düzeltildi, ikinci denemede sorunsuz. Bu hata unit testlerle yakalanamamıştı (DI konteynerini kullanmadıkları için) — kalıcı kural olarak `backend-dev.md`/`reviewer.md`'ye eklendi.

## Engeller
Yok.
