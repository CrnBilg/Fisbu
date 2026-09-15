# ADR-005: HouseholdService/StatisticsService'in Receipt Port'larını Atlaması

**Durum**: Kabul edildi
**Tarih**: 2026-09-15
**İlgili story**: ARCH-003
**Kaynak**: Sprint 2 backend denetimi, bulgu Y2 — `HouseholdService.java:27-29,117-143`, `StatisticsService.java:27-35,74,105-122`

## Bağlam
`HouseholdService` ve `StatisticsService` (ikisi de legacy `com.fisbu.api.service` paketinde), `receipt` modülünün hexagonal port'larını (`com.fisbu.api.receipt.application.port.out.*`) atlayıp legacy `com.fisbu.api.repository.ReceiptRepository` ve `ReceiptItemRepository`'yi doğrudan enjekte ediyor:

- `HouseholdService.getStatistics()` — household üyelerinin (User listesi) belirli bir tarih aralığındaki fişlerini `receiptRepository.findByUserInAndReceiptDateBetween(members, start, end)` ile çekip üye/kategori bazında toplam hesaplıyor (satır 117-143).
- `StatisticsService` — 4 farklı sorgu deseni kullanıyor:
  - `findByUserAndReceiptDateBetween(user, start, end)` (aylık istatistik, tekli ve N-ay aralık)
  - `findByUser(user)` (mağaza istatistiği, abonelik tespiti, harcama kişiliği/rozetler)
  - `receiptItemRepository.findByReceipt_User_Id(user.getId())` (en çok alınan ürünler)

Bu ADR'nin kapsamı ARCH-002/ADR-004'ten (AuthService — silme orkestrasyonu, FK sıralı, tek transaction) niteliksel olarak farklıdır: burada **yalnızca okuma (query/load)** var, silme yok, sıralama/transaction garantisi gerektirmeyen bağımsız sorgular var. Dolayısıyla ADR-004'ün "port + orkestratör servis" çözümü burada birebir uygulanmaz; daha basit bir "sorgu port'u ekleme" yaklaşımı yeterlidir.

Mevcut `receipt` port'larına bakıldığında (`LoadReceiptsPort.loadByUserId(Long userId, int limit)`) bunlar tekil kullanıcı + limit odaklı; HouseholdService'in ihtiyacı olan "çoklu kullanıcı + tarih aralığı" ve StatisticsService'in ihtiyacı olan "tekli kullanıcı + tarih aralığı" ile "tekli kullanıcı, limitsiz, tüm zamanlar" sorgu şekilleri mevcut port'larla karşılanamıyor — yeni port(lar) gerekiyor. Ayrıca `receipt.domain.Receipt` (hexagonal domain modeli) ile legacy `com.fisbu.api.entity.Receipt`/`entity.Category`/`entity.User` arasında alan/ilişki farkı var (örn. `receipt.getCategory().getName()`, `receipt.getUser().getId()` gibi ilişkisel gezinmeler legacy entity'de doğrudan mevcut); yeni port'ların dönüş tipi bu noktada bir tasarım kararı gerektiriyor.

`ReceiptItemRepository.findByReceipt_User_Id` için de aynı soru geçerli: `receipt` modülünde henüz `ReceiptItem` için bir out-port yok.

## Değerlendirilen Seçenekler

### Seçenek 1: Legacy domain modelini (entity.Receipt) döndüren yeni port'lar eklemek
`receipt/application/port/out/` altına `LoadReceiptsByUserAndDateRangePort`, `LoadReceiptsByUsersAndDateRangePort`, `LoadReceiptsByUserPort` gibi port'lar eklenir; port imzaları dönüş tipi olarak **hâlâ legacy `com.fisbu.api.entity.Receipt`** kullanır (adapter, `ReceiptRepository`'yi sarar, aynı entity'yi döner).
Artı: Minimum değişiklik — HouseholdService/StatisticsService'in mevcut kodu (receipt.getCategory(), receipt.getUser() gibi) neredeyse aynen kalır, sadece repository çağrısı port çağrısına döner. Hızlı ve düşük riskli.
Eksi: Port'un dönüş tipi hâlâ legacy entity olduğu için hexagonal'ın "iç domain modeli dışarı sızmaz" garantisi tam sağlanmaz — port sınırı sadece "hangi sınıf enjekte ediliyor" seviyesinde çizilir, "hangi model dışarı taşınıyor" seviyesinde değil. `receipt` modülünün domain modeli (`receipt.domain.Receipt`) ileride değişirse (örn. entity'den ayrışırsa) bu port'lar yine kırılabilir — sorunu tam çözmez, sadece bir katman geciktirir.

### Seçenek 2: `receipt` modülünün hexagonal domain modelini (`receipt.domain.Receipt`) döndüren port'lar eklemek — SEÇİLDİ
Aynı yeni port'lar eklenir ama dönüş tipi `com.fisbu.api.receipt.domain.Receipt` (mevcut `LoadReceiptsPort` ile aynı konvansiyon). `HouseholdService`/`StatisticsService`, kendi ihtiyaç duydukları alanlara (toplam tutar, kategori adı, kullanıcı id/ad/e-posta, mağaza adı, fiş tarihi, split-details, vb.) domain modeli üzerinden erişecek şekilde güncellenir.
Artı: Hexagonal sınırı tam anlamıyla korunur — receipt modülünün iç veri modeli (entity dahil) değişse bile bu iki servis sadece domain modelin public sözleşmesine bağlı kalır, ADR'nin çözmeye çalıştığı asıl riski (Y2 bulgusu) ortadan kaldırır. `LoadReceiptsPort` ile aynı desende, projede tutarlı bir port ailesi oluşur.
Eksi: `receipt.domain.Receipt`'in HouseholdService/StatisticsService'in ihtiyaç duyduğu tüm alanları (kategori adı+id, kullanıcı ad/e-posta, mağaza adı, split-details json, receiptDate) zaten taşıyıp taşımadığı doğrulanmalı; taşımıyorsa domain modeline alan eklemek gerekebilir (küçük ek iş, ama receipt modülünün kendi iç genişlemesi — dış modülün sorumluluğu değil). HouseholdService/StatisticsService'teki hesaplama kodunun `entity.Receipt` → `domain.Receipt` alan adı/erişim farklılıklarına göre küçük ölçüde revize edilmesi gerekir (mekanik ama satır sayısı fazla olan bir değişiklik).

## Karşılaştırma
Seçenek 1, "port var ama içi legacy model" durumunu yaratarak ADR'nin çözmeye çalıştığı asıl sorunu (iç domain değişirse dış modül kırılır) sadece yüzeysel olarak kapatır — bir sonraki denetimde tekrar bulgu olarak çıkar. Seçenek 2 daha fazla mekanik değişiklik gerektirse de, ARCH-002/ADR-004'te benimsenen "port sınırının içi domain modelidir, entity değildir" ilkesiyle tutarlıdır ve projenin hexagonal migrasyon hedefiyle örtüşür. Story'de okuma/agregasyon dışında sıralama veya transaction garantisi gerektiren bir iş olmadığından, ADR-004'teki gibi ayrı bir orkestratör servis eklemeye gerek yoktur — port'lar doğrudan mevcut servislerden çağrılabilir.

## Karar
**Seçenek 2** uygulanır: `receipt` modülüne domain modeli (`receipt.domain.Receipt`) döndüren üç yeni out-port eklenir, `ReceiptItem` için de aynı desende bir port eklenir. `HouseholdService`/`StatisticsService` bu port'ları kullanacak şekilde güncellenir ve `ReceiptRepository`/`ReceiptItemRepository` enjeksiyonları kaldırılır.

### Uygulama Planı (backend-dev için)

1. **Yeni out-port'lar** (`com.fisbu.api.receipt.application.port.out`, mevcut `LoadReceiptsPort` ile aynı konvansiyon — dönüş tipi `receipt.domain.Receipt`):
   - `LoadReceiptsByUserAndDateRangePort` — `List<Receipt> loadByUserIdAndDateRange(Long userId, LocalDate start, LocalDate end)` (StatisticsService: `getMonthlyStatistics`, `getMonthlyStatisticsRange` için; legacy `findByUserAndReceiptDateBetween` karşılığı).
   - `LoadReceiptsByUserIdsAndDateRangePort` — `List<Receipt> loadByUserIdsAndDateRange(List<Long> userIds, LocalDate start, LocalDate end)` (HouseholdService: `getStatistics` için; legacy `findByUserInAndReceiptDateBetween` karşılığı — imza `User` nesnesi değil `userId` listesi alır, böylece receipt modülü `com.fisbu.api.entity.User`'a bağımlı olmaz).
   - `LoadAllReceiptsByUserPort` — `List<Receipt> loadAllByUserId(Long userId)` (StatisticsService: `getStoreStatistics`, `getPotentialSubscriptions`, `getSpendingPersonality` için; legacy `findByUser(user)` karşılığı — `LoadReceiptsPort.loadByUserId`'den farklı olarak limitsiz, çünkü bu üç metod tüm geçmişi analiz ediyor).
   - `com.fisbu.api.receipt.application.port.out.LoadReceiptItemsByUserPort` — `List<ReceiptItem> loadByUserId(Long userId)` (StatisticsService: `getTopProducts` için; legacy `findByReceipt_User_Id` karşılığı). `ReceiptItem` için domain modeli receipt modülünde yoksa, bu port `receipt.domain.ReceiptItem` adında küçük bir domain modeli tanımlanmasını da kapsar (normalizedName, productName, quantity, unitPrice alanları).

2. **Domain modeli kontrolü/genişletmesi**: `receipt.domain.Receipt`'in şu alanları taşıdığından emin olunmalı (yoksa eklenmeli): `totalAmount`, `receiptDate`, `storeName`, `splitDetailsJson`, kategori (id + ad), kullanıcı (id + ad + e-posta). Bu, receipt modülünün kendi iç genişlemesidir, dış modülün sorumluluğu değildir.

3. **Adapter**: mevcut `LoadReceiptsPort` implementasyonunun bulunduğu persistence adapter sınıfına (veya aynı paket altında yeni bir adapter'a) yukarıdaki 4 metod eklenir; her biri ilgili `ReceiptRepository`/`ReceiptItemRepository` sorgusunu çağırıp entity'yi domain modeline map'ler (mevcut entity→domain mapping fonksiyonu varsa yeniden kullanılır).

4. **HouseholdService değişikliği**: `ReceiptRepository receiptRepository` alanı/constructor parametresi kaldırılır, yerine `LoadReceiptsByUserIdsAndDateRangePort` enjekte edilir. Satır 117-118: `userRepository.findByHousehold(household)` sonucundan `userId` listesi çıkarılıp port çağrılır. Satır 133-143'teki döngü, `domain.Receipt`'in alan erişimlerine göre (örn. `receipt.getCategory().getName()` yerine domain modelin karşılık gelen erişimi) güncellenir; hesaplama mantığı (toplam, üye/kategori bazlı gruplama) değişmez.

5. **StatisticsService değişikliği**: `ReceiptRepository receiptRepository` ve `ReceiptItemRepository receiptItemRepository` alanları/constructor parametreleri kaldırılır, yerine `LoadReceiptsByUserAndDateRangePort`, `LoadAllReceiptsByUserPort`, `LoadReceiptItemsByUserPort` enjekte edilir:
   - Satır 74: `findByUserAndReceiptDateBetween` → `LoadReceiptsByUserAndDateRangePort.loadByUserIdAndDateRange`.
   - Satır 86: aynı port, range sorgusu için.
   - Satır 139, 203, 279: `findByUser(user)` → `LoadAllReceiptsByUserPort.loadAllByUserId`.
   - Satır 173: `receiptItemRepository.findByReceipt_User_Id` → `LoadReceiptItemsByUserPort.loadByUserId`.
   - Hesaplama/agregasyon mantığı (`buildMonthlyStatistics`, `computeSubscriptionCandidates`, `computePersona`, `computeBadges`) değişmez, sadece girdi tipinin (`entity.Receipt` → `domain.Receipt`) alan erişim yolları güncellenir.

6. **Regresyon testi (ÖNCE, kod değişmeden)**: `getStatistics` (HouseholdService) ve `getMonthlyStatistics`/`getStoreStatistics`/`getTopProducts`/`getPotentialSubscriptions`/`getSpendingPersonality` (StatisticsService) için mevcut davranışı (verilen fiş/fiş kalemi verisiyle üretilen tam çıktıyı) kilitleyen testler yazılır; refactor sonrası aynı testler aynı sonucu doğrulamalı.

## Sonuç
`HouseholdService` ve `StatisticsService` artık `ReceiptRepository`/`ReceiptItemRepository`'yi doğrudan enjekte etmez; receipt modülünün iç domain modeli değişse bile bu iki servis sadece port sözleşmesine (domain modeli döndüren port imzaları) bağlı kalır. ADR-004'ten farklı olarak burada sıralama/transaction garantisi gerekmediği için ayrı bir orkestratör servis eklenmez — port'lar doğrudan mevcut servislerden çağrılır. `ReceiptItem` için receipt modülünde bir domain modeli/port yoksa, bu ADR kapsamında küçük ölçekte eklenir; bu, `ReceiptItem`'ın tam hexagonal migrasyonu anlamına gelmez (o, kapsamı daha büyük ayrı bir çalışma olabilir).
