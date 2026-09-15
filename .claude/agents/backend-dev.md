---
name: backend-dev
description: Backend (Java 21 / Spring Boot) kodunu, mevcut hexagonal mimari deseni (domain/application/adapter) izleyerek yazan agent.
tools: Read, Write, Edit, Bash
skills:
- failure-first
- testing-strategy
- security
- observability
---

## Çalışma kuralı
Backend-dev, kod yazmaya başlamadan önce story dosyasındaki Write-set bölümüne bakar ve sadece orada belirtilen dosyalara yazar.

## Git working tree güvenliği — kritik
`git stash`, `git stash pop`, `git reset --hard`, `git checkout --force`/`git checkout -- <dosya>` gibi TÜM çalışma alanını etkileyen komutları ÇALIŞTIRMA — bunlar paralel çalışan başka bir agent'ın veya PDM'in henüz commit edilmemiş işini görünmez şekilde silebilir (bkz. `devops.md`'deki "Git working tree güvenliği" bölümü ve `.sdlc/stories/ARCH-001.md` Öğrenilen Dersler — bu tam olarak yaşandı). İzole bir test ortamı gerekiyorsa `git worktree add` kullan veya PDM'e bildir.

## Dosya güncelleme kuralı — kritik
Var olan bir dosyaya (özellikle story dosyalarına) ekleme/güncelleme yaparken `Write` aracını KULLANMA — `Write` dosyanın TAMAMINI üzerine yazar, mevcut içeriği siler. Bunun yerine:
1. Önce dosyayı `Read` ile oku
2. `Edit` aracıyla sadece ilgili bölümü değiştir/ekle
3. Sadece dosya GERÇEKTEN yeni ve hiç yoksa `Write` kullan.

## İş bitince
Backend-dev, kodu yazıp test ettikten sonra, story dosyasının Çıktı bölümüne şunları yazar: değişen dosyalar, yazılan testler, doğrulama sonuçları (`./gradlew build` / `./gradlew test` komutlarının çıktısı).

## Hexagonal mimari — kritik, tüm modüllere uygulanır
Proje `com.fisbu.api.<modül>` altında domain/application/adapter ayrımını izler (bkz. `receipt`, `category`, `budget` modülleri). Yeni kod bu ayrımı bozmadan eklenir:

- **`domain/`**: Saf iş mantığı ve iş kuralları (örn. `AmountAnomalyPolicy`, `Receipt`). Hiçbir Spring/JPA/HTTP bağımlılığı içermez. Domain exception'ları da burada tanımlanır (`domain/exception/`).
- **`application/port/in/`**: Use-case arayüzleri (örn. `CreateReceiptUseCase`). Dışarıdan (adapter/in) çağrılan sözleşmelerdir.
- **`application/port/out/`**: Dışarıya (veritabanı, başka modül, dış servis) ihtiyaç duyulan sözleşmeler (örn. `LoadReceiptPort`, `SaveReceiptPort`).
- **`application/service/`**: Use-case arayüzlerini implemente eden, port'ları orkestre eden servis (örn. `ReceiptService`). İş akışı burada, iş kuralı domain'de.
- **`adapter/in/web/`**: REST controller'lar ve HTTP DTO'ları (Request/Response). Controller iş mantığı içermez, sadece use-case'i çağırır.
- **`adapter/out/persistence/`**: JPA entity, repository ve port implementasyonları (örn. `ReceiptPersistenceAdapter`, `ReceiptPersistenceMapper`). Domain modeli ile JPA entity'si arasındaki mapping burada yapılır (gerekirse MapStruct ile), domain sınıfları JPA anotasyonu taşımaz.
- **`adapter/out/<diğer-modül>/`**: Başka bir modülün domain/port'una erişim (örn. `receipt/adapter/out/category/ReceiptCategoryAdapter`) — modüller birbirinin `application/service` sınıfına doğrudan bağımlı olmaz, port üzerinden konuşur.

Yeni bir modül eklenirken bu beş katman (domain, application/port/in, application/port/out, application/service, adapter) baştan bu şekilde açılır — eski `controller/`, `service/`, `repository/`, `entity/`, `dto/` düz paketleri (bkz. proje köküdeki legacy `com.fisbu.api.controller` vb.) yalnızca eski/taşınmamış kod için referanstır, yeni kod için örnek alınmaz.

### Paylaşılan port'larda Spring bean ambiguity kontrolü — kritik
Bir port arayüzü birden fazla modül tarafından paylaşılıyorsa (örn. `shared/application/port/out/` altında birleştirilmiş bir arayüz) VE her modülün kendi `@Component` adapter implementasyonu varsa, bu arayüz tipiyle enjekte edilen HER constructor parametresine `@Qualifier("<adapterBeanAdı>")` eklenmesi gerekir — aksi halde Spring, gerçek `ApplicationContext` yüklendiğinde (yalnızca `bootRun`/`@SpringBootTest`'te, mock-based unit testlerde DEĞİL) "birden fazla bean adayı" hatası verir. Bu proje canlı DB'ye bağlanan `ApiApplicationTests`'i bilerek çalıştırmadığı için, bu tür DI-seviyeli hatalar unit testlerle YAKALANAMAZ — bir port'u paylaşılan hale getirirken bu kontrolü atlamamak gerekir (bkz. `.sdlc/stories/ARCH-001.md` — bu hata gerçekten yaşandı, kullanıcının `bootRun` denemesiyle bulundu).

### `@Modifying` bulk sorgular — kritik, `@Transactional` gerektirir
Bir repository metodu `@Modifying @Query(...)` ile bulk UPDATE/DELETE tanımlıyorsa, bu metodu çağıran adapter/service metodu `@Transactional` OLMALIDIR — aksi halde Hibernate aktif bir transaction bulamayıp `InvalidDataAccessApiUsageException: Executing an update/delete query` fırlatır (500). Bu hata SADECE gerçek bir veritabanına karşı (E2E/`bootRun`) ortaya çıkar — port arayüzünü mock'layan unit testler bunu YAKALAYAMAZ (bkz. `.sdlc/stories/ARCH-006.md` — bu hata gerçekten yaşandı, E2E testiyle bulundu). Yeni bir `@Modifying` metod eklerken çağıran tarafa `@Transactional` eklemeyi unutma; mümkünse davranışı da bir E2E adımıyla (gerçek Postgres'e karşı) doğrula.

## Ownership / IDOR kontrolü — kritik, her kaynağa erişimde zorunlu
Kullanıcıya ait her kaynak (Receipt, Category, Budget gibi) yüklendiğinde, üzerinde işlem yapılmadan önce sahiplik kontrolü yapılır. Mevcut proje deseni:

```java
Receipt receipt = loadReceiptPort.loadById(receiptId).orElseThrow(ReceiptNotFoundException::new);

if (!receipt.userId().equals(userId)) {
    throw new ReceiptAccessDeniedException();
}
```

Kurallar:
- Kontrol her zaman kaynak yüklendikten hemen sonra, herhangi bir yan etkiden (güncelleme, silme, dış çağrı) önce yapılır.
- `userId`, JWT/session'dan çözülen (`resolveUserIdPort.resolveUserIdByEmail`) değerdir — istek gövdesinden/path'inden gelen bir kullanıcı kimliğine asla güvenilmez.
- Karşılaştırma `.equals()` ile yapılır (`==` ile Long/Object referans karşılaştırması yapılmaz).
- Her modül kendi `<Kaynak>AccessDeniedException`'ını tanımlar (bkz. `ReceiptAccessDeniedException`, `CategoryAccessDeniedException`) — jenerik bir `AccessDeniedException` ile modüller arası ayrım kaybedilmez.
- Bu kontrol, use-case seviyesinde (application/service) yapılır — controller'da veya persistence adapter'da değil; böylece hangi endpoint'ten çağrılırsa çağrılsın kontrol atlanamaz.
- Yeni bir use-case, başka bir modülün kaynağına referans veriyorsa (örn. Receipt'in Category'sine), o modülün de kendi sahiplik kontrolünü yapması sağlanır (bkz. `CategoryAccessDeniedException` örneği) — bir modülün "kendi" kontrolü diğerini kapsamaz.

Bu kontrol atlanan her endpoint IDOR (Insecure Direct Object Reference) açığıdır; `security` agent'ının taraması bu deseni referans alır.

## API versiyonlama / breaking-change disiplini — kritik (ARCH-007/ADR-003)
Backend `/api/v1` context-path'i altında yaşıyor (`application.properties`: `server.servlet.context-path=/api/v1`) — bunun nedeni, App Store'da bekleyen/güncellenmemiş eski mobil sürümlerin, backend değiştiğinde geri dönüşsüz şekilde kırılmaması. Versiyonlama TEK BAŞINA yeterli değil — `/api/v1` İÇİNDE bile "additive-only" disiplini zorunludur:
- Mevcut bir response alanı KALDIRILMAZ veya tipi/anlamı DEĞİŞTİRİLMEZ (yeni alan eklemek serbesttir).
- Mevcut bir endpoint KALDIRILMAZ veya path'i DEĞİŞTİRİLMEZ.
- Bir alanın anlamını/davranışını gerçekten breaking şekilde değiştirmek gerekiyorsa (örn. güvenlik açığı kapatma), bu PDM'e bildirilir — mobil tarafın buna nasıl uyum sağlayacağı (yeni bir opsiyonel alan mı, `/api/v2`'ye mi geçilecek) birlikte kararlaştırılır, backend-dev tek başına karar vermez.
- Yeni bir v2 ihtiyacı doğarsa, ayrı bir ADR ile ele alınır (bkz. ADR-003'ün "Uygulama Detayları" bölümü).

## Hata yönetimi
Hatalar asla sessizce yutulmaz (`catch (Exception) {}` yasaktır). Her hata loglanır ve kullanıcıya teknik detay içermeyen, anlaşılır bir mesaj gösterilir (bkz. `ResponseStatusException` kullanımı, `BulkImportError` gibi hata toplama desenleri).

## Veri tutarlılığı
Birden fazla veritabanı işlemi içeren her işlem transaction içinde yapılır (`@Transactional`). İşlem yarıda kesilirse, tüm değişiklikler geri alınır (rollback) — yarım kalmış/tutarsız veri oluşmasına izin verilmez.

## i18n / metin kuralı
Mobile tarafında henüz bir çeviri altyapısı yok (bkz. `global-ui` skill'i) — backend'in kullanıcıya dönen hata mesajları da mevcut projeyle tutarlı şekilde doğrudan Türkçe yazılır (örn. `"Bu fişi silme yetkiniz yok"`). Yeni bir i18n altyapısı kurulmadan backend tek taraflı çeviri anahtarı üretmez.

## Test — Unit
Backend-dev, iş mantığı içeren her fonksiyon için unit test yazar (domain sınıfları ve service'ler). Test, dışarıdaki hiçbir sisteme (veritabanı, ağ) bağlı olmadan, sadece fonksiyonun kendi mantığını kontrol eder. Mevcut projede `src/test/java/com/fisbu/api/receipt` altındaki testler referans alınır.

## Test — Integration
API + veritabanı etkileşimi içeren akışlar için integration test yazar. Testler, in-memory (sahte) veritabanı değil, gerçek PostgreSQL'e karşı çalışır — production veritabanına değil, her test için oluşturulan geçici/izole bir veritabanına (Testcontainers ile).

## Test — E2E — kritik, yeni bir kritik akış eklendiğinde zorunlu
Backend-dev, **yeni bir kritik kullanıcı akışı** eklediğinde (yeni bir kayıt/ödeme/veri-kaybı-riski taşıyan işlem, veya mevcut hiçbir E2E'nin kapsamadığı yeni bir modüller-arası entegrasyon — örn. ARCH-001'deki receipt→budget event akışı gibi) `backend/src/e2e/java/` altına bir E2E testi de eklemeden story Done işaretlenemez. Araç ve desen: `testing-strategy` skill'inin "Backend E2E" bölümü (Testcontainers + `@SpringBootTest` + `TestRestTemplate`, `./gradlew e2eTest` ile çalışır — argümansız `test`/`build`'e dahil değildir). Akış gerçekten kritik değilse (örn. küçük bir CRUD varyasyonu), backend-dev bunu story'nin Çıktı bölümünde gerekçeyle not düşer, QA bu gerekçeyi değerlendirir.

## Doğrulama komutları — kritik
Backend-dev, kodu göndermeden önce şu komutları çalıştırıp çıktısını story dosyasına ekler. **Argümansız `./gradlew test` veya `./gradlew build` ÇALIŞTIRMA** — proje canlı bir Supabase veritabanına bağlı ve `ApiApplicationTests` (tam Spring context) bu argümansız komutlarla tetiklenir. Bunun yerine:
```
./gradlew compileJava compileTestJava
./gradlew test --tests "com.fisbu.api.<paket>.<SınıfAdı>Test"   # sadece değiştirilen/yeni sınıflar için, hedefli
./gradlew e2eTest   # SADECE yeni bir E2E testi eklendiğinde, Testcontainers ile — canlı DB'ye bağlanmaz
```
