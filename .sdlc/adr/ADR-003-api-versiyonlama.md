**Durum**: Kabul edildi
**Tarih**: 2026-09-13
**Kaynak**: ARCH-007 (PDM tarafından çağrıldı; Sprint 2 backend denetimi bulgu A2)

## Bağlam

Backend'deki hiçbir controller'da (`AuthController` → `/auth`, `BudgetController` → `/budgets`, `ReceiptController` → `/receipts` vb.) bir API versiyon segmenti yok; path'ler düz kök seviyesinde (`/auth/login`, `/budgets/{id}` gibi). Mobil taraf da (`mobile/lib/services/api_client.dart`) bu varsayımla yazılmış: `baseUrl` (production URL) ile controller'dan gelen path doğrudan string concat ile birleştiriliyor (`Uri.parse('$baseUrl$path')`), aralarında versiyon segmenti yok.

Proje App Store'a yayınlanmaya hazırlanıyor. Mağaza review/güncelleme döngüsü günler-haftalar sürebilir; bu süre boyunca eski mobil istemci sürümleri production backend'e karşı çalışmaya devam edecek. Backend'de dağıtılan HERHANGİ bir breaking değişiklik (alan kaldırma/yeniden adlandırma, response şekli değişikliği, endpoint kaldırma), mağazada bekleyen veya henüz güncellenmemiş TÜM eski istemcileri anında ve geri dönüşsüz kırar — kod zaten kullanıcı cihazlarında dağıtılmış olacağı için hotfix/rollback mobil tarafta mümkün değildir. Bu risk pencere App Store submission'ından önce kapatılmalı.

Mevcut yapıda daha önce benzer bir konuda (backend Java 21/Spring Boot 3.5.15, hexagonal mimariye geçiş) mimari kararlar ADR olarak `.sdlc/adr/` altına kaydediliyor; bu ADR de aynı formatı takip eder.

## Değerlendirilen Seçenekler

### Seçenek 1 — URL Path Versiyonlama (`/api/v1/...`)
**Artıları:**
- En yaygın, endüstri standardı yaklaşım; anlaşılması ve debug edilmesi (curl/Postman/browser'da doğrudan görülebilir) en kolay yöntem.
- Mobil tarafta değişiklik minimal: `ApiClient.baseUrl`'e sabit bir `/api/v1` segmenti eklemek (veya `_uri()` içinde path'e prefix eklemek) yeterli — her çağrı sitesinde (`get`, `post`, `put`, `delete`, `postMultipart`) tek bir merkezi noktadan yönetiliyor zaten.
- Spring tarafında da tek satırlık bir çözüm var: `server.servlet.context-path=/api/v1` (application.properties) — hiçbir controller'ın `@RequestMapping` anotasyonuna dokunmaya gerek kalmaz.
- Gelecekte gerçekten breaking bir değişiklik gerekirse (`/api/v2`), eski ve yeni versiyonun aynı anda, ayrı context-path/controller paketiyle yaşayabilmesine izin verir — geriye dönük uyumluluk için net bir kaçış yolu sağlar.

**Eksileri:**
- "Saf REST" felsefesine göre versiyon URI'nin bir parçası olmamalı (kaynak kimliği ile versiyon karışıyor) — ama bu proje ölçeğinde pragmatik fayda bu teorik kaygıdan ağır basıyor.
- Gerçek bir v2 gerektiğinde controller/route çoğaltması (veya en azından routing karmaşıklığı) gerekir — ama bu, versiyonlamanın doğası gereği zaten kaçınılmaz bir maliyet.

### Seçenek 2 — Header Bazlı Versiyonlama (`Accept: application/vnd.fisbu.v1+json`)
**Artıları:**
- Daha "temiz" REST: kaynak URI'si versiyon bağımsız kalır, içerik pazarlığı (content negotiation) standardına uyar.

**Eksileri:**
- Mobil tarafta her tek istekte (`get`/`post`/`put`/`delete`/`postMultipart`) header enjekte etmek gerekir — `_headers()` metoduna ek mantık eklense de, curl/Postman ile manuel test veya debug her seferinde ekstra header hatırlamayı gerektirir; ekibin günlük hızını düşürür.
- Spring tarafında `produces`/`consumes` bazlı content negotiation kurmak, path bazlı çözümden belirgin şekilde daha fazla boilerplate ve daha kırılgan yapılandırma gerektirir (her controller metodunda ya da merkezi bir `RequestMappingHandlerMapping` özelleştirmesiyle).
- Hata ayıklama ve log okunabilirliği daha zor: path versiyonlamada versiyon log satırında/URL'de görünür, header versiyonlamada görünmez — bu da ARCH-007'nin doğrudan ilgili olduğu "production'da hızlı teşhis" ihtiyacını zayıflatır.
- Takımın deneyimi ve proje ölçeği göz önüne alındığında getirdiği "temizlik" faydası, artan operasyonel karmaşıklığı haklı çıkarmıyor.

### Seçenek 3 — Versiyonlama Yok, Sadece "Additive-Only" Disiplini
**Artıları:**
- En düşük efor: hiçbir kod değişikliği gerekmez, sadece `backend-dev.md`'ye bir kural eklenir.
- Küçük bir ekip için kısa vadede yeterli olabilir.

**Eksileri:**
- Tamamen insan disiplinine bağlı, hiçbir teknik güvence sağlamıyor — bir geliştiricinin (veya gelecekte otonom bir ajanın) bir DTO alanını "sadece küçük bir düzeltme" diyerek değiştirmesi tüm mağazadaki eski istemcileri anında kırabilir ve bu geri alınamaz.
- "Additive-only" kuralını sonsuza kadar sürdürmek pratikte imkânsız: bazı düzeltmeler (ör. güvenlik açığı kapatma, yanlış response şekli) doğası gereği breaking'dir; bu seçenek gerçek bir breaking change ihtiyacı doğduğunda hiçbir kaçış yolu bırakmaz.
- App Store'a çıkmadan önce kapatılması istenen risk penceresini kapatmaz, sadece "dikkatli olun" der — ARCH-007'nin zamanlama gerekçesiyle doğrudan çelişir.

## Karar

**Seçenek 1 — URL Path Versiyonlama (`/api/v1`)** seçildi.

Gerekçe: Mobil tarafta değişiklik maliyeti en düşük seçenek bu (tek satırlık `baseUrl` güncellemesi, `ApiClient` zaten tüm istekleri tek bir merkezi noktadan geçiriyor). Backend tarafında tüm controller'lara tutarlı şekilde tek bir yapılandırma satırıyla (`server.servlet.context-path`) uygulanabiliyor, controller kodlarına dokunmaya gerek yok — bu da "kısmi uygulama" riskini (Kabul Kriteri #2) ortadan kaldırıyor çünkü context-path tüm route'lara otomatik ve tutarlı şekilde uygulanır. Header bazlı versiyonlamanın "temizlik" avantajı, bu ölçekteki bir projede debug/test/log okunabilirliği pahasına haklı çıkmıyor. Versiyonsuz "additive-only" disiplini ise App Store'un getirdiği geri-dönüşsüzlük riskini teknik olarak hiç azaltmıyor, sadece süreç kuralına güveniyor — bu, ARCH-007'nin tam olarak önlemeye çalıştığı senaryo.

## Uygulama Detayları (backend-dev/mobile-dev tarafından uygulanacak)

### Backend
`backend/src/main/resources/application.properties` (ve varsa profil bazlı eşdeğerleri, örn. `application-test.properties`) içine:
```properties
server.servlet.context-path=/api/v1
```
eklenir. Hiçbir controller'ın `@RequestMapping` anotasyonu değişmez (`/auth`, `/budgets`, `/receipts` aynı kalır) — Spring bu path'lerin önüne context-path'i otomatik ekler. Sonuç: `/auth/login` → `/api/v1/auth/login`, `/budgets/{id}` → `/api/v1/budgets/{id}` gibi tüm endpoint'lere otomatik ve tutarlı biçimde uygulanır.

E2E testler (`backend/src/e2e/`) ve `RegisterToBudgetFlowE2ETest`, context-path uygulandıktan sonra path'leri `/api/v1/...` olarak güncellemeli veya test client'ının base URL'ine context-path'i dahil etmelidir.

Gelecekte gerçek bir v2 ihtiyacı doğarsa (ör. mobil tarafın eski sürümlerinin tamamen sahadan kalktığı bir noktada breaking bir değişiklik gerekirse), yeni bir context-path (`/api/v2`) altında ayrı bir Spring profili/uygulama yapılandırması veya versiyon bazlı ayrı controller paketi ile ele alınabilir — bu ADR'nin kapsamı dışında, ihtiyaç doğduğunda ayrı bir ADR ile ele alınmalıdır.

### Mobil (`mobile/lib/services/api_client.dart`)
Tek değişiklik `baseUrl` sabitinde:

Önce:
```dart
static const String baseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'https://fisbu-production-613c.up.railway.app',
);
```

Sonra:
```dart
static const String baseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'https://fisbu-production-613c.up.railway.app/api/v1',
);
```

`_uri()` metoduna veya çağıranlara (`get`/`post`/`put`/`delete`/`postMultipart`) dokunmaya gerek yok — path'ler zaten controller'lardaki gibi (`/auth/login`, `/budgets/{id}`) kalır, `baseUrl` + path birleşimi otomatik olarak `.../api/v1/auth/login` üretir. E2E test override'ı (`--dart-define=API_BASE_URL=...`) kullanan yerel/ephemeral backend'lerin de context-path'i URL'lerine dahil etmesi gerekir (backend context-path uyguladığı için).

## Sonuçlar

- **Olumlu**: App Store submission'ından önce, backend'de gelecekte gerekebilecek bir v2'ye geçiş için net bir kaçış yolu var; mevcut mobil sürümler `/api/v1` altında sabitlenmiş kalıyor, backend'in `v2`'ye geçmesi eski sürümleri kırmaz.
- **Olumlu**: Uygulama maliyeti çok düşük — backend'de tek satır config, mobilde tek satır sabit değişikliği. Kısmi/eksik uygulama riski yok çünkü context-path tüm route'lara otomatik uygulanıyor.
- **Nötr/İzlenmesi gereken**: E2E testlerin (backend ve mobil) path'leri/base URL'leri güncellenmeli; bu ADR'nin Kabul Kriterleri'nde zaten not edilmiş (ARCH-007).
- **Olumsuz/Kabul edilen risk**: `/api/v1` içinde bile additive-only disiplini (yeni alan ekleme, mevcut alanı kaldırmama) hâlâ backend-dev.md'ye bir kural olarak eklenmelidir — versiyonlama, v1 içinde disiplinsiz breaking değişiklikleri engellemez, sadece v1→v2 geçişi için bir kaçış yolu sağlar. Bu nedenle Seçenek 3'ün "additive-only" disiplini burada TAMAMLAYICI olarak (versiyonlamanın yerine değil, yanında) backend-dev.md'ye eklenmesi önerilir.
