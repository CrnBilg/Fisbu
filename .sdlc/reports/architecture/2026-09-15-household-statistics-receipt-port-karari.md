# Mimari Karar Özeti: HouseholdService/StatisticsService Receipt Port Bypass (ARCH-003)

**Tarih**: 2026-09-15
**ADR**: `.sdlc/adr/ADR-005-household-statistics-receipt-port-bypass.md`

## Sorun
`HouseholdService` ve `StatisticsService` (legacy `com.fisbu.api.service`), `receipt` modülünün hexagonal port'larını atlayıp `ReceiptRepository`/`ReceiptItemRepository`'yi doğrudan kullanıyordu. ARCH-002/ADR-004 ile aynı kategori (domain sınır ihlali) ama farklı nitelik: burada sadece okuma/agregasyon var, silme/sıralı orkestrasyon yok.

## Karar
`receipt` modülüne domain modeli (`receipt.domain.Receipt`, gerekirse yeni `receipt.domain.ReceiptItem`) döndüren 4 yeni out-port eklenir:
- `LoadReceiptsByUserAndDateRangePort`
- `LoadReceiptsByUserIdsAndDateRangePort`
- `LoadAllReceiptsByUserPort`
- `LoadReceiptItemsByUserPort`

Legacy entity döndüren "yüzeysel" port yerine (Seçenek 1, reddedildi), domain modeli döndüren port'lar seçildi (Seçenek 2) — böylece receipt modülünün iç veri modeli değişse bile bu iki servis kırılmaz. ADR-004'ten farklı olarak sıralama/transaction garantisi gerekmediğinden ayrı bir orkestratör servis eklenmiyor; port'lar doğrudan mevcut servislerden çağrılıyor.

## Değişecek Dosyalar (uygulama backend-dev'e bırakıldı)
- Yeni: `backend/src/main/java/com/fisbu/api/receipt/application/port/out/LoadReceiptsByUserAndDateRangePort.java`
- Yeni: `backend/src/main/java/com/fisbu/api/receipt/application/port/out/LoadReceiptsByUserIdsAndDateRangePort.java`
- Yeni: `backend/src/main/java/com/fisbu/api/receipt/application/port/out/LoadAllReceiptsByUserPort.java`
- Yeni: `backend/src/main/java/com/fisbu/api/receipt/application/port/out/LoadReceiptItemsByUserPort.java`
- Değişecek (muhtemelen yeni): `receipt` modülünün mevcut persistence adapter sınıfı (yukarıdaki 4 port'un implementasyonu eklenir)
- Değişecek: `backend/src/main/java/com/fisbu/api/service/HouseholdService.java` (satır 27-29, 44-52, 117-143)
- Değişecek: `backend/src/main/java/com/fisbu/api/service/StatisticsService.java` (satır 27-35, 49-62, 74, 86, 139, 173, 203, 279)
- Gerekirse genişletilecek: `receipt.domain.Receipt` (eksik alan varsa) ve yeni `receipt.domain.ReceiptItem`
- Yeni/güncellenecek: regresyon testleri (mevcut davranışı kilitlemek için, refactor öncesi yazılmalı)

## Not
`ReceiptItem` için receipt modülünde domain modeli/port bu ADR kapsamında küçük ölçekte eklenir; `ReceiptItem`'ın tam hexagonal migrasyonu bu ADR'nin kapsamı dışındadır.
