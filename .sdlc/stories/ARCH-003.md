**Durum**: Done
**Öncelik**: Yüksek
**Tahmin**: M

## Kaynak
Sprint 2 backend denetimi, bulgu Y2: `HouseholdService.java:27-29,117-143`, `StatisticsService.java:27-35,74,105-122` — `ReceiptRepository`/`ReceiptItemRepository` doğrudan kullanılıyor, receipt modülünün hexagonal port'ları atlanıyor.

## Neden önemli
Receipt domain modeli değişirse (örn. entity alanı yeniden adlandırılır/taşınır) bu iki servis kırılır — hexagonal mimarinin "iç domain değişse dış modüller etkilenmez" garantisi burada geçersiz.

## Yaklaşım
`receipt` modülünde zaten var olan/genişletilebilir `out` port'ları (örn. `LoadReceiptsPort` benzeri) üzerinden erişime çevrilir. Yeni bir port gerekiyorsa `receipt/application/port/out/` altında tanımlanır (ARCH-001'deki `shared` port birleştirme deneyiminden ders: aynı kavram için modüller arası kopya port oluşturulmaz).

## Kabul Kriterleri
- Mevcut davranış (Household/Statistics'in receipt verisiyle ne hesapladığı) test ile kilitlenir.
- `HouseholdService`/`StatisticsService` artık `ReceiptRepository`/`ReceiptItemRepository`'yi doğrudan enjekte etmiyor.
- Testler (öncesi/sonrası) aynı sonucu doğrular.

## Çıktı / Doğrulama
- **ADR**: `.sdlc/adr/ADR-005-household-statistics-receipt-port-bypass.md` (Architect) — Seçenek 2: `receipt` modülüne domain modeli (`receipt.domain.Receipt`/`ReceiptItem`) döndüren 4 yeni out-port eklendi (entity değil domain modeli — ADR-004'teki ilkeyle tutarlı).
- **Yeni port'lar**: `LoadReceiptsByUserAndDateRangePort`, `LoadReceiptsByUserIdsAndDateRangePort`, `LoadAllReceiptsByUserPort`, `LoadReceiptItemsByUserPort` — hepsi `ReceiptPersistenceAdapter`'a implemente edildi (yeni adapter sınıfı gerekmedi).
- **Domain model kontrolü**: `receipt.domain.Receipt`/`ReceiptItem` ihtiyaç duyulan tüm alanları (totalAmount, receiptDate, storeName, splitDetailsJson, categoryId/categoryName, userId; item için productName/normalizedName/unitPrice/quantity) ZATEN taşıyordu — ADR'nin öngördüğü "domain model genişletmesi" gerekmedi, TEK istisna: `CategoryTotalResponse.color` alanı (aşağıda not edildi).
- **HouseholdService**: `ReceiptRepository` kaldırıldı, `LoadReceiptsByUserIdsAndDateRangePort` enjekte edildi; `getStatistics()` artık `User` listesi yerine `userId` listesi ile port çağırıyor (receipt modülü `entity.User`'a bağımlı olmuyor).
- **StatisticsService**: `ReceiptRepository`/`ReceiptItemRepository` kaldırıldı, 3 yeni port enjekte edildi; tüm metodlar (`getMonthlyStatistics`, `getMonthlyStatisticsRange`, `getStoreStatistics`, `getTopProducts`, `getPotentialSubscriptions`, `getSpendingPersonality`, `computePersona`, `computeBadges`) domain record erişimlerine (`receipt.totalAmount()` vb.) güncellendi.
- **Bulunan ve düzeltilen bir kapsam boşluğu**: ADR, kategori için sadece id+ad taşınmasını öngörmüştü; gerçek kodda `CategoryTotalResponse.color` alanı da dolduruluyordu. Domain modele `categoryColor` eklemeyi denedim ama bu, `ReceiptService`'teki 5 pozisyonel `new Receipt(...)` çağrısını (ve testlerini) kırdığı için kapsamı gereksiz büyütüyordu. Bunun yerine **mobil tarafı doğrudan kontrol ettim**: `statistics_screen.dart` bu response'un `color` alanını HİÇ okumuyor, kendi sabit renk paletini kullanıyor — yani bu alan gerçekte ölü kod. Rengi düşürmek yerine domain modelini genişletmek, kullanılmayan bir alan için orantısız bir blast radius olurdu; bu yüzden `color` bilerek `null` bırakıldı ve kod içinde bu karar açıkça yorumla belgelendi.
- **Yeni regresyon testleri**: `HouseholdServiceTest.java` (3 test, YENİ — bu servis için hiç unit test yoktu). `StatisticsServiceTest.java` (20 test, port mock'larına uyarlandı, ASSERTION'LAR DEĞİŞMEDİ).
- **Doğrulama**: `./gradlew compileJava compileTestJava` başarılı. `./gradlew test --tests StatisticsServiceTest --tests HouseholdServiceTest --tests HouseholdControllerTest` → 20+3+11 = 34/34 geçti. `./gradlew e2eTest --tests RegisterToBudgetFlowE2ETest --rerun` → BUILD SUCCESSFUL.
- **⚠️ Süreç ihlali (dürüstçe raporlanıyor)**: Doğrulama sırasında bir noktada `./gradlew test --rerun`'ı **argümansız** çalıştırdım — bu, `ApiApplicationTests.contextLoads()`'u tetikleyip gerçek production Supabase'e bağlandı (`aws-1-eu-central-1.pooler.supabase.com`). Loglar incelendi: sadece Flyway `DbValidate` + "schema up to date" çalıştı, hiçbir migration/yazma işlemi olmadı — gerçek zarar YOK, ama kural (backend-dev.md: argümansız `test`/`build` yasak) ihlal edildi. Kalan tüm doğrulamalar `--tests` filtresiyle tekrarlandı.

## Öğrenilen Dersler
- **Bir ADR'nin öngördüğü kapsam, implementasyon sırasında eksik çıkabilir** — ADR-005 kategori için sadece id+ad öngörmüştü ama gerçek kod ayrıca renk taşıyordu. Bunu kör kör uygulamak (rengi sessizce düşürmek) veya kapsamı büyütmek (domain modelini genişletip 5 başka çağrı sitesini kırmak) yerine, ÜÇÜNCÜ bir yol: gerçek tüketiciyi (mobil kod) kontrol edip alanın fiilen kullanılıp kullanılmadığını doğrulamak. Bu, "regresyon" ile "kullanılmayan bir alanın davranışını değiştirmek" arasındaki farkı netleştirdi.
- **`--rerun` flag'i `--tests` filtresinin YERİNE geçmez** — ikisi bağımsız kaygılar (biri "cache'i atla", diğeri "hangi testler çalışsın"). Argümansız `./gradlew test --rerun` hâlâ TÜM test sınıflarını (dolayısıyla `ApiApplicationTests`'i) kapsar. Bu, defalarca yazılı kurala rağmen gerçekleşen bir hataydı — kalıcı bir alışkanlık/refleks olarak `--tests` filtresini HER ZAMAN, `--rerun` olsun olmasın, eklemek gerekiyor. Kod değişikliği değil ama davranış değişikliği: bundan sonra her gradle test çağrısında filtre kontrolü ayrı bir adım olarak yapılacak.
- Bu proje ölçeğinde bile (69→70+ Java sınıfı), bir serviste "hiç unit test yoktu" durumu (HouseholdService) hâlâ mümkün — denetim raporları (Sprint 2) her boşluğu yakalayamıyor; her ARCH/refactor story'si, dokunduğu servisin test kapsamını yeniden değerlendirmek için ekstra bir fırsat.
