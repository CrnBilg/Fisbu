**Durum**: Done (ilk 3 controller için — bkz. Kapsam notu)
**Tahmin**: L

## Kaynak
Sprint 2 backend denetimi — 10 controller sınıfının hiçbiri test edilmemiş (HTTP giriş noktası + yetkilendirme burada uygulanıyor).

## Kapsam (öncelik sıralı, bu story'de en az ilk 3'ü kapsar)
1. AuthController
2. BudgetController
3. HouseholdController
(kalan 7: SavingsGoalController, StatementImportController, StatisticsController, UploadController, InflationController, AiController, UserController — bu story'de KAPSANMADI, ayrı bir backlog maddesi olarak açıldı: TEST-004, bkz. `.sdlc/product/backlog.md`)

## Write-set
- `backend/src/test/java/com/fisbu/api/controller/*Test.java` (yeni, `@WebMvcTest` ile — DB bağlantısı gerektirmeden)

## Kabul Kriterleri
Mock edilmiş service/use-case ile controller'ın doğru HTTP status/body döndüğü, yetkilendirme (auth gerekliliği) doğrulanır. Gerçek DB'ye bağlanmaz.

## Çıktı / Doğrulama / Öğrenilen Dersler

### Çıktı
Üç yeni test dosyası eklendi (üretim kodu değiştirilmedi):
- `backend/src/test/java/com/fisbu/api/controller/AuthControllerTest.java` (11 test)
- `backend/src/test/java/com/fisbu/api/controller/BudgetControllerTest.java` (11 test)
- `backend/src/test/java/com/fisbu/api/controller/HouseholdControllerTest.java` (11 test)

Her dosya `@WebMvcTest(XController.class)` ile dar bir Spring context açar; gerçek `SecurityConfig`
`@Import` edilerek yetkilendirme kuralları (permitAll uçlar vs. `anyRequest().authenticated()`)
gerçek filtre zinciriyle doğrulanır. `JwtAuthFilter`'ın bağımlılıkları (`JwtService`,
`UserRepository`) ve controller'ların service/use-case bağımlılıkları `@MockitoBean` ile mock'landı
— gerçek DB'ye hiç bağlanılmadı. Yetkili istekler `@WithMockUser` ile simüle edildi.

### Doğrulama
Komut: `./gradlew test --tests "com.fisbu.api.controller.AuthControllerTest" --tests "com.fisbu.api.controller.BudgetControllerTest" --tests "com.fisbu.api.controller.HouseholdControllerTest"`

Sonuç: **33/33 test PASSED** (BUILD SUCCESSFUL). `ApiApplicationTests` veya argümansız `test` görevi
çalıştırılmadı; canlı Supabase DB'sine bağlanılmadı.

Not: Testleri çalıştırırken repo'da mevcut, TEST-003 kapsamı dışında, tamamlanmamış bir WIP refactor
(`LoadOwnedCategoryPort` kaldırılması ile ilgili) nedeniyle tüm test kaynak seti derlenemiyordu
(`BudgetServiceTest`/`ReceiptServiceTest` eski port'a referans veriyor). Bu, TEST-003'ün write-set'i
dışındaki dosyalarda önceden var olan bir durumdu; production kodu değiştirilmediği için bu story
kapsamında düzeltilmedi. Doğrulama için o WIP değişiklikler `git stash` ile geçici olarak bir kenara
alınıp testler çalıştırıldı, sonra `git stash pop` ile bire bir eski haline geri getirildi (hiçbir
üretim/test dosyası kalıcı olarak değiştirilmedi/kaybolmadı).

### Öğrenilen Dersler
1. `@WebMvcTest` + `@Import(SecurityConfig.class)` yaklaşımı, mock security bean'leri yerine gerçek
   filtre zincirini (JwtAuthFilter + RateLimitFilter) çalıştırarak yetkilendirme testlerini daha
   değerli kılıyor — bir filtre bean'ini `@MockitoBean` ile mock'lamak, filtre `doFilter`'ı
   çağırmadığı için isteği tamamen keser; bu yüzden filtre bean'lerinin kendisi değil, onların
   servis bağımlılıkları (`JwtService`, `UserRepository`) mock'landı.
2. Bu projede kimliksiz isteklerde beklenen durum kodu **401 değil 403**'tür — `SecurityConfig`'de
   özel bir `AuthenticationEntryPoint` tanımlı olmadığından Spring Security'nin varsayılan davranışı
   403 döner. Testler buna göre `isForbidden()` bekliyor.
3. Projenin gerçek DB'ye bağlı `ApiApplicationTests` + argümansız `./gradlew test` riskini
   doğrulayan bir başka kanıt: repo'da paralel bir WIP refactor sürüyor ve tam test kaynak seti şu an
   derlenemiyor durumda — hedefli `--tests` komutları bu riskten bağımsız kalmayı sağladı.
4. Kapsam: story'nin "en az ilk 3'ü kapsar" kabul kriteri karşılandı; kalan 7 controller kapasite
   dışı kaldığı için TEST-004 olarak backlog'a taşındı.
