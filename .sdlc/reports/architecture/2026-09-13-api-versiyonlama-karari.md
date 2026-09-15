# API Versiyonlama Kararı — Özet

**Tarih**: 2026-09-13
**İlgili story**: ARCH-007
**ADR**: `.sdlc/adr/ADR-003-api-versiyonlama.md`

## Karar
URL path bazlı versiyonlama seçildi: `/api/v1`.

## Neden
- Mobil tarafta en düşük maliyet: `ApiClient.baseUrl`'e tek satırlık bir `/api/v1` eklemesi yeterli, `_uri()`/istek metodlarına dokunulmuyor.
- Backend tarafında tek bir `server.servlet.context-path=/api/v1` config satırı tüm controller'lara (`/auth`, `/budgets`, `/receipts` vb.) otomatik ve tutarlı biçimde uygulanıyor — kısmi uygulama riski yok.
- Header bazlı versiyonlama (Accept header) mobilde her istekte ek header gerektiriyor ve debug/curl/Postman testlerini zorlaştırıyor; bu ölçekteki proje için "temizlik" faydası operasyonel maliyeti karşılamıyor.
- Versiyonsuz "additive-only" disiplini App Store'un getirdiği geri-dönüşsüz kırılma riskini teknik olarak azaltmıyor, sadece insan disiplinine güveniyor.

## Uygulama (ADR'de detaylı)
- Backend: `application.properties`'e `server.servlet.context-path=/api/v1` eklenir, controller anotasyonları değişmez.
- Mobil: `api_client.dart`'taki `baseUrl` default değerine `/api/v1` eklenir.
- E2E testler (backend + mobil) yeni path'lerle güncellenmeli.
- Tamamlayıcı olarak: additive-only disiplini de backend-dev.md'ye eklenmesi önerildi (versiyonlamanın yerine değil, yanında).
