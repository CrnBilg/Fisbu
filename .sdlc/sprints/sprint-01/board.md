# Sprint 1

**Sprint Hedefi**: Güvenlik denetimi (SEC-001..004) bulgularını kapatmak: ownership/IDOR taraması, Flyway migration disiplinini kurmak, AuthService regresyon testleriyle güvence altına almak, ve deploy gate'inin gerçekten aktif olduğunu doğrulamak.

**Kapasite notu**: 4 madde de bir önceki güvenlik denetiminden geliyor. SEC-001, kod değişikliği içermeyen bir tarama/rapor işi olduğu için en güvenli başlangıç noktası olarak seçildi ve ilk sırada başlatılıyor. SEC-002/003/004 write-set'leri birbirinden ve SEC-001'den bağımsız olduğu için, SEC-001 tamamlandıktan sonra sırayla veya (kapasiteye göre) paralel değerlendirilebilir.

## Story'ler

| ID | Başlık | Tahmin | Sıralama Notu | Durum |
|---|---|---|---|---|
| SEC-001 | Eski paket ownership/IDOR taraması | S | Bağımsız, kod değişikliği yok — ilk çalıştırılan | Done |
| SEC-002 | Flyway migration disiplini kurulumu | M | Tamamlandı — kullanıcı kendi ortamında bootRun'ı başarıyla çalıştırdı, `flyway_schema_history`'de `version=1, type=BASELINE, success=true` kaydı doğrulandı | Done |
| SEC-003 | AuthService regresyon testleri | S | Tamamlandı — 25/25 test geçti, `AuthService.java` değiştirilmedi | Done |
| SEC-004 | Deploy gate doğrulaması (repo dışı ayar) | S | Tamamlandı — CI var ama deploy'u gate etmiyor (branch protection/ruleset/environment protection yok); ayrıca bugün bir prod deploy'unun "failure" olduğu tesadüfen bulundu | Done |
| SEC-005 | Dependency CVE düzeltmeleri (postgresql, poi-ooxml) | S | SEC-001'i tamamlayan ek taramada bulundu (2026-09-10) | Draft |
| SEC-006 | Log'larda PII (email) sızıntısı | S | SEC-001'i tamamlayan ek taramada bulundu (2026-09-10) | Draft |

## Akış (her story için)
- SEC-001: `security` (PDM sadece bu agent'ı çağırır — kod değişikliği yok)
- SEC-002: `devops` → `qa` → `reviewer`
- SEC-003: `backend-dev` → `qa` → `reviewer`
- SEC-004: `devops` (tespit/rapor) → PDM (kullanıcıya iletim)

## Engeller
Yok — SEC-002'deki "password authentication failed" engeli kullanıcı tarafından kendi ortamında aşıldı (kök neden kesin teşhis edilemedi ama sonuç doğrulandı, bkz. SEC-002.md).

## Bilgilendirme (SEC-004'ten yan bulgu)
2026-09-10 07:29-07:32 arası production'da bir deploy denemesi "failure" ile sonuçlanmış (Railway). Bu, sprint'in bir engeli değil ama kullanıcıya iletilen ayrı bir operasyonel uyarı.
