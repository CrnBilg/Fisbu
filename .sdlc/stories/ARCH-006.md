**Durum**: Done
**Öncelik**: Orta-Yüksek (kapsam daraltıldı — gerekçe aşağıda)
**Tahmin**: S-M

## Kaynak
Sprint 2 backend denetimi, bulgu Y7: 10+ repository/port metodu pagination almıyor: `ReceiptRepository.findByUser/findByCategory`, `ReceiptItemRepository` (2 metod), `BudgetRepository` (3 metod), `SavingsGoalRepository.findByUser`, `CategoryRepository.findByUser`, `LoadBudgetsPort` (2 metod), `LoadCategoriesPort.loadByUserId`, `FindReceiptsByStoreNameContainingPort`, `FindDuplicateReceiptPort`.

## Önceliklendirme gerekçesi (kapsam daraltma)
10 metodun hepsi eşit risk taşımıyor — kullanıcı başına gerçekçi veri hacmine göre ayrıştırıldı:
- **Gerçek risk (bu story'nin kapsamı)**: `ReceiptRepository.findByUser/findByCategory`, `ReceiptItemRepository` (2 metod), `FindReceiptsByStoreNameContainingPort`, `FindDuplicateReceiptPort` — Receipt/ReceiptItem, projedeki tek gerçek anlamda **sınırsız büyüyen** (yıllar içinde birikimli, işlemsel) veri. `ReceiptService`'te zaten `MAX_RECEIPTS_LIST=2000` deseni var (`LoadReceiptsPort.loadByUserId` örneği) — bu desen eksik kalan metodlara yaygınlaştırılacak.
- **Düşük risk (kapsam dışı, backlog'a not düşülür, DÜŞÜK öncelikle ayrı ele alınabilir)**: `BudgetRepository` (3 metod, ay×kategori başına — kullanıcı başına gerçekçi üst sınır birkaç yüz), `SavingsGoalRepository.findByUser` (kullanıcı başına birkaç hedef), `CategoryRepository.findByUser`/`LoadCategoriesPort.loadByUserId` (kullanıcı başına birkaç düzine kategori) — bunlarda pagination eklemek gerçek bir performans kazancı sağlamaz, sadece API yüzeyini karmaşıklaştırır.

## Kabul Kriterleri
- Kapsamdaki 6 metoda `Pageable`/limit parametresi eklenir (mevcut `LoadReceiptsPort.loadByUserId` deseniyle tutarlı).
- Bu metodları çağıran servis/controller'lar güncellenir (varsayılan sayfa boyutu, mevcut `MAX_RECEIPTS_LIST=2000` ile tutarlı olacak şekilde).
- Mevcut davranış (limit'siz çağrıldığında dönen veri) test ile önce kilitlenir, sonra limit eklenmiş haliyle karşılaştırılır.
- Düşük-risk 6 metod, gerekçesiyle backlog'da DÜŞÜK öncelikli ayrı bir madde (PAGINATION-LOW veya benzeri) olarak not düşülür, bu story'de değiştirilmez.

## Çıktı / Doğrulama
- **Yeniden önceliklendirme (kod yazmadan önce yapıldı)**: Orijinal 6 metodun gerçek çağrı noktaları incelendi, sadece 2'sinin gerçek/pratik risk taşıdığı görüldü:
  1. **`ReceiptRepository.findByCategory`** (kategori silindiğinde `unlinkCategoryFromReceipts`'te kullanılıyordu) — kategoriyi yüzlerce/binlerce fişte kullanan bir kullanıcı için TÜM fişleri belleğe çekip tek tek `setCategory(null)` + `saveAll` yapıyordu. **Çözüm**: pagination değil, bulk `UPDATE` sorgusu (`unlinkCategoryFromAllReceipts`) — davranış birebir aynı, bellek/zaman maliyeti O(n)'den O(1) sorguya düştü.
  2. **`FindReceiptsByStoreNameContainingPort`** (kategori önerisi `suggestCategory`'de kullanılıyordu) — bir mağazadan yüzlerce kez alışveriş yapan kullanıcı için tüm eşleşmeleri çekiyordu. **Çözüm**: en güncel 50 fişle örnekleme (`MAX_CATEGORY_SUGGESTION_SAMPLE`) — öneri amacına yeterli, davranış pratikte aynı (çoğu kullanıcı bir mağazadan 50'den az fiş biriktirir).
  3. **`FindDuplicateReceiptPort`**: ZATEN doğal olarak bounded (tam mağaza+tutar+tarih eşleşmesi arıyor, sonuç seti pratikte 0-2 kayıt) — pagination gerçek bir fayda sağlamıyor, dokunulmadı.
  4. **`ReceiptItemRepository` (2 metod, `PersonalInflationService`/istatistik agregasyonu için)**: bunlar `LoadAllReceiptsByUserPort` (ARCH-003) ile aynı kategoride — DOĞRU çözüm pagination değil, DB-seviyesinde agregasyon (PERF-002 deseni gibi SQL SUM/GROUP BY). Bu, bu story'nin "port'a limit ekle" kapsamını aşan, ayrı ve daha büyük bir performans çalışması — backlog'a not düşüldü (aşağıya bakınız), kapsam dışı bırakıldı.
- **Kod değişiklikleri**:
  - `ReceiptRepository.java`: yeni `@Modifying @Query unlinkCategoryFromAllReceipts(Category)`.
  - `ReceiptPersistenceAdapter.unlinkCategoryFromReceipts`: `findByCategory`+`saveAll` yerine tek bulk UPDATE çağrısı; `@Transactional` eklendi (Hibernate'in `@Modifying` sorguları için aktif transaction gerektirmesi nedeniyle — bu gerçek bir E2E çalıştırmasıyla bulundu, bkz. Öğrenilen Dersler).
  - `ReceiptPersistenceAdapter.findByUserIdAndStoreNameContaining`: `Pageable`/limit (50, en güncel önce) eklendi.
- **Yeni/genişletilmiş test**: Mevcut unit testler (port arayüzünü mock'layan `CategoryServiceTest`/`ReceiptServiceTest`) bu adaptör-seviyesi değişikliği KAPSAMIYOR — bu yüzden `RegisterToBudgetFlowE2ETest`'e 2 yeni adım eklendi (adım 7: kategori sil, adım 8: fişin kategorisinin gerçekten NULL olduğunu GET ile doğrula). Bu, `@Modifying` bulk UPDATE'in gerçek Postgres'e karşı çalıştığının tek kanıtı.
- **Bulunan gerçek bir hata (E2E ile yakalandı)**: İlk implementasyon `@Transactional` içermiyordu — Hibernate "Executing an update/delete query" (`InvalidDataAccessApiUsageException`) ile 500 döndürdü. Sadece unit testlerle (port mock'landığı için) bu YAKALANAMAZDI; gerçek DB'ye karşı E2E testinin değerini bir kez daha kanıtladı.
- **Doğrulama**: `./gradlew compileJava compileTestJava` başarılı. `./gradlew test --tests ReceiptServiceTest --tests CategoryServiceTest --tests "com.fisbu.api.controller.*"` (filtreyle, argümansız DEĞİL) → tüm sınıflar geçti, regresyon yok. `./gradlew e2eTest --tests RegisterToBudgetFlowE2ETest --rerun` → 8/8 adım geçti (2 yeni adım dahil).
- **Backlog'a not düşülen, kapsam dışı bırakılan iş**: `PersonalInflationService`/`StatisticsService`'in tüm-geçmiş-yükleyip-Java'da-agregasyon deseni (orijinal denetimin ayrı bir bulgusu, Y4) — gerçek çözüm DB-seviyesinde SUM/GROUP BY, bu story'nin "port'lara limit ekle" kapsamını aşıyor. `PERF-003` deseninde olduğu gibi backlog'a düşürüldü, gerçek şikayet/performans sorunu gerçekleşirse ele alınacak.

## Öğrenilen Dersler
- "Pagination ekle" talimatı her risk için doğru çözüm değil — bazı durumlarda (kategori-fiş bağlantısını kaldırma gibi TÜM kayıtları etkilemesi gereken bir işlem) doğru çözüm bulk bir DB işlemi, sayfalama değil (sayfalama burada "her sayfada işlem yapıp N kez tekrarla" gibi daha kötü bir çözüme yol açardı). Kabul kriterini kör kör uygulamadan önce her metodun GERÇEK kullanım amacını (listeleme mi, toplu-işlem mi, agregasyon mu) sormak doğru çözümü belirledi.
- `@Modifying` bulk sorgularının çağıran tarafında (ya adapter'da ya da service'te) AÇIK bir `@Transactional` gerektirdiği — ve bunun mock-based unit testlerle YAKALANAMAYACAĞI, sadece gerçek bir veritabanına karşı çalışan bir testle (E2E) ortaya çıkacağı — bu projede artık `backend-dev.md`'ye eklenmeye değer bir kural (ARCH-006'nın en somut, tekrar kullanılabilir dersi).
- Bir story'nin kapsamını daraltırken "düşük risk" diye elenen metodların bir kısmının aslında BAŞKA bir story'nin (Y4/PersonalInflationService) parçası olduğu ortaya çıktı — bu, denetim bulgularının birbiriyle örtüşebileceğini, bir story'yi kapatırken diğerine sızan işi backlog'da açık ve izlenebilir tutmanın (burada yapıldığı gibi) önemini gösteriyor.
