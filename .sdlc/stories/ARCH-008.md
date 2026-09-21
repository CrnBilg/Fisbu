**Durum**: Done
**Öncelik**: Yüksek (Grup 1 işlevsel denetim, madde 2)
**Tahmin**: M-L (backend + mobil)

## Kaynak
Grup 1 işlevsel denetiminde bulunan bulgu: `receipt_detail_screen.dart`'ta mağaza adı/tutar/tarih/kategori DÜZENLENEMİYORDU — OCR yanlış okuduysa ya da kullanıcı bir yazım hatası fark ettiyse, tek seçenek fişi silip baştan eklemekti. Araştırma sırasında backend'de de hiçbir update endpoint'i olmadığı ortaya çıktı (sadece `POST /receipts`, `DELETE /receipts/{id}`, `PUT /receipts/{id}/split`, `PUT /receipts/{id}/reminders`). Kullanıcıya bu kapsam genişlemesi (sadece mobil değil, backend de) soruldu, "Backend + mobil, tam kapsam" onaylandı.

## Kabul Kriterleri
- Backend'de `PUT /receipts/{id}` endpoint'i (mağaza/tutar/tarih/kategori günceller) hexagonal mimari deseniyle (domain/application/adapter) eklenmeli, ownership (hem fiş hem kategori) doğrulanmalı.
- Mobilde `AddReceiptScreen` "düzenle" modunda yeniden kullanılmalı — fotoğraf/ürün alanları gizlenmeli (bu alanlar düzenlemeyi desteklemiyor).
- `receipt_detail_screen.dart`'a bir düzenle girişi (AppBar ikonu) eklenmeli.
- Her iki tarafta da test + kanıt.

## Çıktı / Doğrulama

### Backend
- **`UpdateReceiptUseCase`** (yeni in-port): `updateReceipt(email, receiptId, storeName, totalAmount, receiptDate, categoryId)`.
- **`UpdateReceiptRequest`** (yeni DTO): `ReceiptRequest`'in doğrulama desenini (`@NotBlank`/`@Size`/`@DecimalMin`/`@DecimalMax`/`@NotNull`) aynen kullanıyor, ama sadece 4 alan taşıyor (fotoğraf/ürün/allowDuplicate yok — bunlar update kapsamında değil).
- **`ReceiptService.updateReceipt`**: `setReminders`/`saveSplit` ile AYNI desen — `getOwnedReceipt` ile fiş sahipliği doğrulanır, `categoryId` verilmişse `loadOwnedCategoryPort.loadById` ile kategori sahipliği doğrulanır (createReceipt'teki AYNI mantık), sonra immutable `Receipt` record'u yeni alanlarla yeniden inşa edilip `saveReceiptPort.save` ile kaydedilir.
- **`ReceiptController`**: `PUT /receipts/{id}` endpoint'i eklendi.
- **Testler** (`ReceiptServiceTest`, 5 yeni test): çekirdek alanların güncellendiği (kategori yokken), sahip olunan kategori verildiğinde kategori adının/id'sinin set edildiği, fiş başka kullanıcıya aitse `ReceiptAccessDeniedException`, kategori başka kullanıcıya aitse `CategoryAccessDeniedException`, fiş yoksa `ReceiptNotFoundException` fırlatıldığı doğrulandı.
- **Doğrulama**: `ReceiptServiceTest` → 23/23 geçti (5 yeni). Tam backend suite (argümansız `./gradlew test` KULLANILMADI — standart kural gereği `ApiApplicationTests` hariç 27 sınıf `--tests` ile açıkça çalıştırıldı) → **247/247 geçti**, regresyon yok.

### Mobil
- **`ReceiptService.updateReceipt`** (yeni): `PUT /receipts/$id` çağırıyor, `saveSplit`/`setReminders` ile aynı hata/başarı işleme deseni.
- **`AddReceiptScreen`**: yeni `editingReceiptId` parametresi. Verildiğinde: AppBar başlığı "Fişi Düzenle", fotoğraf seçim alanı VE "Ürünler (opsiyonel)" bölümü tamamen gizli (bu ikisi düzenlemeyi desteklemiyor — kullanıcıya var olmayan bir yeteneği varmış gibi göstermemek için), "Kaydet" butonu yeni `_handleUpdate()` metodunu çağırıyor (`_handleSave`'den ayrı — duplicate-check/fotoğraf-yükleme/ürün mantığı düzenleme akışına karışmasın diye). Başarıda `SuccessCheckOverlay` (MOB-028) gösterilip güncellenmiş `Receipt` nesnesiyle `Navigator.pop` yapılıyor.
- **`receipt_detail_screen.dart`**: AppBar'a `Icons.edit_outlined` ikonu eklendi, `_editReceipt` mevcut fişin alanlarını `initialX` parametreleriyle `AddReceiptScreen`'e taşıyor, dönüşte `result is Receipt` ise `_receipt` state'i güncelleniyor (ekstra bir network çağrısı gerekmeden).
- **Testler** (`add_receipt_screen_test.dart`, 5 yeni test): başlığın moda göre doğru değiştiği, düzenleme modunda fotoğraf/ürün bölümlerinin GİZLENDİĞİ, normal modda GÖRÜNDÜĞÜ, alanların initial değerlerle dolu geldiği doğrulandı.
- **Görsel doğrulama (gerçek emulator, `emulator-5554`)**: `receipt_detail_screen.dart`'taki kalem ikonuna GERÇEKTEN dokunulup `AddReceiptScreen`'in "Fişi Düzenle" başlığıyla, doldurulmuş alanlarla ve gizlenmiş fotoğraf/ürün bölümleriyle açıldığı uçtan uca kanıtlandı (debug harness'te önceden hazırlanmış bir ekran değil, gerçek `Navigator.push` akışı).
- **Doğrulama**: `flutter analyze lib test` → 3 (kapsam dışı, değişmedi). `flutter test` → 61/61 geçti (10 yeni: 5 backend testine ek mobil tarafta 5), regresyon yok.

## Öğrenilen Dersler
- Bir "sadece mobil UI" gibi görünen bir talebin altında GERÇEKTE bir backend eksikliği yatabilir — kod tabanını "bu özellik zaten var mı" diye kontrol etmeden (burada `ReceiptController`'ın endpoint listesini grep'lemeden) kapsamı küçük varsaymak, yarım/yanlış bir teslimat riski taşırdı. Kapsam sürprizini erken (kod yazmadan önce) fark edip kullanıcıya sormak, kod yazdıktan sonra "aslında backend'e de dokunmam gerekiyormuş" demekten çok daha ucuza geldi.
- `AddReceiptScreen` gibi çok-amaçlı bir formu "düzenle" modunda yeniden kullanırken, formun DESTEKLEMEDİĞİ alanları (fotoğraf, ürünler) sessizce görünür bırakıp arka planda yok saymak yerine TAMAMEN GİZLEMEK, kullanıcıya yanlış bir "bunu da değiştirebilirim" izlenimi vermeyi önlüyor — UI'da olmayan bir özelliği ima etmemek, olan bir özelliği eksik göstermekten daha güvenli bir varsayılan.
