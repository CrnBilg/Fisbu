**Durum**: Done
**Öncelik**: Acil (rapordaki orijinal önceliği — kullanıcı Yüksek olarak sıraladı, App Store zamanlaması nedeniyle Acil'e yükseltilmesi öneriliyor, aşağıya bakınız)
**Tahmin**: L (ADR + tüm controller'lara uygulama + mobil taraf koordinasyonu)

## Kaynak
Sprint 2 backend denetimi, bulgu A2 (rapordaki önceliği ACİL — kullanıcının bu görüşmede verdiği sırada 6. madde/Yüksek, aşağıda gerekçesiyle tekrar ACİL öneriliyor): hiçbir controller'da `/api/v1` gibi bir prefix yok, mobil taraf da versiyon segmenti kullanmıyor.

## Neden zamanlama kritik
App Store review süreci genelde 1-3 gün ama reddedilme/güncelleme döngüleri haftalar sürebilir — bu süre boyunca ESKİ mobil sürüm canlıda kalmaya devam eder. Versiyonlama App Store'a çıkmadan ÖNCE kurulmazsa, ilk backend breaking-change'inde mağazadaki onay bekleyen/henüz güncellenmemiş sürümler anında kırılır ve geriye dönük düzeltme şansı yoktur (kod zaten dağıtılmış). Bu yüzden PDM olarak bu maddenin **App Store submission'ından önce** tamamlanmasını öneriyorum — kullanıcının sıralamasında son sırada olsa da takvim baskısı en yüksek madde bu.

## Yaklaşım
Kod değiştirmeden önce **Architect'e danışılacak** — seçenekler:
1. URL path versiyonlama (`/api/v1/receipts` gibi) — en yaygın, mobil `ApiClient.baseUrl`'e tek bir path prefix eklemek yeterli, en düşük mobil-taraf maliyeti.
2. Header bazlı versiyonlama (`Accept: application/vnd.fisbu.v1+json`) — daha "temiz" REST ama mobil tarafta her istekte header eklemek gerekir, debug/test sırasında (curl/Postman) daha zahmetli.
3. Versiyonlama yapmayıp SADECE additive-only (asla breaking change yapmama) disiplinini backend-dev.md'ye kural olarak eklemek — en düşük efor ama disiplin bağımlı, insan hatasına açık.
Karar **ADR olarak** `.sdlc/adr/`'a yazılacak; mobil tarafta (`ApiClient.baseUrl`) hangi değişikliğin gerekeceği de ADR'de netleştirilecek.

## Kabul Kriterleri
- ADR yazılıp karar gerekçelendirilir.
- Seçilen strateji TÜM controller'lara tutarlı şekilde uygulanır (kısmi uygulama, versiyonlamanın amacını boşa çıkarır).
- Mobil taraf (`ApiClient.baseUrl`/istek header'ları) yeni stratejiyle güncellenir, mevcut E2E testleri (backend + mobil) hâlâ geçer.
- Backend E2E testi (`RegisterToBudgetFlowE2ETest`) yeni versiyonlu path'lerle çalışır.

## Çıktı / Doğrulama
- **ADR**: `.sdlc/adr/ADR-003-api-versiyonlama.md` (Architect tarafından yazıldı) — Seçenek 1 (URL path, `/api/v1`) seçildi. Özet rapor: `.sdlc/reports/architecture/2026-09-13-api-versiyonlama-karari.md`.
- **Backend**: `backend/src/main/resources/application.properties`'e `server.servlet.context-path=/api/v1` eklendi. Hiçbir `@RequestMapping` değişmedi — Spring context-path'i tüm route'lara otomatik uyguluyor.
- **Doğrulama (öncesi/sonrası)**: `./gradlew e2eTest --rerun` değişiklikten ÖNCE çalıştırıldı (baseline: BUILD SUCCESSFUL), context-path eklendikten SONRA tekrar çalıştırıldı (BUILD SUCCESSFUL, regresyon yok). **Beklenmedik ama olumlu bulgu**: `RegisterToBudgetFlowE2ETest`'teki `TestRestTemplate` (Spring Boot'un otomatik yapılandırdığı, `@SpringBootTest(webEnvironment=RANDOM_PORT)` ile) context-path'i OTOMATİK algılıyor — ADR'nin öngördüğü "E2E testlerin path'leri güncellenmeli" adımı GEREKMEDİ, test kodunda tek satır bile değişmedi.
- **Mobil**: `mobile/lib/services/api_client.dart`'taki `baseUrl` default değerine `/api/v1` eklendi (ADR'deki tam kod örneğiyle birebir). `flutter test` → 8/8 geçti, regresyon yok.
- **Dokümantasyon güncellemeleri**: `mobile/integration_test/receipt_to_budget_flow_test.dart`'taki örnek komut yorumu, `.claude/skills/testing-strategy/SKILL.md`'deki E2E override notu — ikisi de artık `/api/v1` segmentinin dahil edilmesi gerektiğini hatırlatıyor (aksi halde yerel/ephemeral backend'e karşı çalışan E2E'ler 404 alır).
- **Kalıcı kural**: `backend-dev.md`'ye "API versiyonlama / breaking-change disiplini" bölümü eklendi — ADR-003'ün önerdiği gibi, versiyonlamanın YANINDA (yerine değil) additive-only disiplini backend-dev'in her zaman uyacağı bir kural olarak yazıldı.

## Öğrenilen Dersler
- Spring Boot'un `@SpringBootTest(webEnvironment=RANDOM_PORT)` ile otomatik yapılandırdığı `TestRestTemplate`, `server.servlet.context-path`'i kendiliğinden root URI'sine dahil ediyor — bu, ADR yazılırken (mimari karar aşamasında) bilinmesi zor, ancak UYGULAMA sırasında (gerçek `e2eTest` çalıştırılarak) hemen ortaya çıkan bir Spring Boot detayı. Bu, "ADR'de öngörülen her uygulama adımının mutlaka gerekli olacağını varsaymamak, gerçek doğrulamayı çalıştırıp görmek" ilkesini bir kez daha doğruluyor — E2E-001'de test altyapısı kurulmasının, tam olarak böyle sürprizleri ucuza yakalamak için değerli olduğu somut bir örnek daha.
- Versiyonlama kararı SADECE bir "gelecekte v2'ye kaçış yolu" değil, aynı zamanda ekibe (ve backend-dev agent'ına) "v1 içinde bile breaking change yapma" disiplinini KURUMSALLAŞTIRMA fırsatı — ADR'nin bunu ayrı bir "olumsuz/kabul edilen risk" olarak işaretleyip tamamlayıcı bir kural önermesi, versiyonlamanın kendi başına yeterli bir teknik çözüm olmadığını, süreç kuralıyla tamamlanması gerektiğini net biçimde ortaya koydu.
