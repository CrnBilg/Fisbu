# Güvenlik taraması — 2026-09-10 (SEC-001'i tamamlayan ek tarama)

**Kapsam**: SEC-001'in kapsamadığı 3 alan — mobil taraf (WebView/secure storage/deep-link), log/hata çıktısı sızıntısı, dependency CVE (backend Maven + mobile Pub).

**Kapsam dışı**: Bu taramanın kapsamadığı alanlar — network/TLS konfigürasyonu, backend'deki authentication/authorization mantığının kendisi (bkz. SEC-001/SEC-003), infra/Railway ayarları (bkz. SEC-004). Bu alanlarda yeni bir açık olup olmadığı bu tur taranmadı.

## 1. Mobil taraf — sonuç: temiz
- WebView kullanımı yok (`grep` sonucu boş).
- JWT token `flutter_secure_storage` üzerinden saklanıyor (`auth_service.dart:6-7,19,46`).
- Hive box'ları (`receiptsBox`, `budgetsBox`, `categoriesBox`, `pendingReceiptsBox`) AES ile şifreleniyor, şifreleme anahtarı `flutter_secure_storage`'da tutuluyor (`local_cache_service.dart:15-24,56-63`) — eski şifresiz box'lardan güvenli migrasyon da var.
- Deep-link/URL scheme kaydı yok (`AndroidManifest.xml`'de sadece `MAIN`/`LAUNCHER` intent-filter'ı var) — mevcut haliyle deep-link kaynaklı bir açık yüzeyi yok.
- Yeni backlog maddesi eklenmedi.

## 2. Log/hata sızıntısı — sonuç: 1 Orta bulgu
- `GlobalExceptionHandler.java`: generic 500 handler'ı stack trace'i client'a DÖNDÜRMÜYOR, sadece server log'una yazıyor (`log.error("Beklenmedik hata", ex)`) — client'a giden mesaj hep jenerik ("Sunucu hatası oluştu"). İyi.
- Request/response body veya `Authorization` header'ını loglayan bir filter yok.
- **Bulgu**: `WeeklySummaryScheduler.java:59` ve `EmailService.java:94`, `log.error(...)` çağrısında kullanıcının e-posta adresini (PII) düz metin logluyor. Şifre/token değil ama backlog'a SEC-006 olarak eklendi (Orta).

## 3. Dependency CVE — sonuç: 2 bulgu (OSV.dev API ile doğrulandı, tahmini değil)
- `org.postgresql:postgresql` (resolved: 42.7.11) → **CVE-2026-54291 (HIGH)**: `channelBinding=require` bağlantılarında SCRAM channel-binding sessizce düşürülüyor. `application.properties`'te `channelBinding=require` set edilmemiş (varsayılan `prefer`) olduğu için şu an aktif olarak sömürülebilir değil, ama sürüm yükseltmesi ucuz ve doğru olan çözüm. SEC-005'e eklendi (Yüksek).
- `org.apache.poi:poi-ooxml` (5.2.5) → **GHSA-gmg8-593g-7mv3 (LOW)**: OOXML zip'lerinde duplicate entry parsing tutarsızlığı. SEC-005'e eklendi.
- Diğer taranan bağımlılıklar (jjwt, firebase-admin, openpdf, pdfbox, commons-csv, mapstruct, cloudinary, spring-boot; mobile: http, crypto, flutter_secure_storage, image, path_provider, file_picker, hive, mime, http_parser, share_plus, connectivity_plus, image_picker) için OSV.dev'de bilinen bir açık bulunamadı.

## Kapsam sınırı hatırlatması
Bu tarama sadece yukarıdaki 3 alanı kapsar. "Bulgu yok" denen her yer (mobil taraf) sadece BU taramanın kapsamında sorun bulunmadığı anlamına gelir — genel bir güvenlik onayı değildir (bkz. `security.md` — Kapsam sınırı kuralı).
