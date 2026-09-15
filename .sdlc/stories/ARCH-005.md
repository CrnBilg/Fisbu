**Durum**: Done
**Öncelik**: Orta (kullanıcının önerdiği Yüksek'ten düşürüldü — gerekçe aşağıda)
**Tahmin**: S

## Kaynak
Sprint 2 backend denetimi, bulgu Y5: `UploadController.java:36-38` — `userDetails` parametre olarak alınıyor ama KULLANILMIYOR; Cloudinary'ye sabit klasöre (`fisbu/receipts`) yükleniyor, hangi kullanıcının ne yüklediği hiçbir yerde kaydedilmiyor.

## Ön inceleme (bu story'nin kapsamını belirlemek için yapıldı, salt-okunur)
`UploadController.java`'nın tamamı okundu. Bulgular:
- Controller'da **listeleme veya silme endpoint'i YOK** — sadece `POST /receipts/upload` var, yanıt olarak Cloudinary'nin ürettiği `secure_url`'i döner. Cloudinary public_id'leri tahmin edilemez (rastgele) olduğundan, bu endpoint üzerinden **başka bir kullanıcının dosyasını görme/silme riski doğrudan YOK** — kullanıcının sorduğu "IDOR riski" bu spesifik endpoint'te bulunamadı.
- Ancak gerçek ve doğrulanmış 2 gap var:
  1. **Audit/quota/abuse-tracking eksikliği** (denetim raporunun orijinal bulgusu): hangi kullanıcının ne zaman ne yüklediği hiçbir yerde yok — kötüye kullanım (spam upload) tespit edilemez, kota uygulanamaz.
  2. **Hesap silme, Cloudinary görsellerini temizlemiyor** (bu incelemede YENİ bulundu): `grep -rln "Cloudinary"` sonucu sadece `CloudinaryConfig`, `UploadController`, `ExportService` — hiçbirinde silme çağrısı yok. `AuthService.deleteAccount()` DB kayıtlarını silse de, kullanıcının yüklediği fiş görselleri Cloudinary'de KVKK/GDPR "verilerimi sil" beklentisine rağmen kalıcı olarak kalıyor. Bu, orijinal denetimde işaretlenmemiş, KVKK açısından potansiyel olarak Y5'ten daha önemli bir bulgu.

## Kabul Kriterleri
- `UploadController.uploadReceiptImage`, yükleyen kullanıcının kimliğini (en azından `userId`) Cloudinary'ye `public_id`/klasör yapısına (`fisbu/receipts/{userId}/...`) veya ayrı bir DB kaydına (hangisi daha uygunsa — küçük kapsamlı bir karar, ADR gerektirmez) işler.
- Hesap silme akışı (`AuthService.deleteAccount`), kullanıcının Cloudinary'ye yüklediği görselleri de siler (yeni kabul kriteri, kullanıcı onayı gerektirir — kapsamı genişletiyor).
- Mevcut upload davranışı (başarılı yanıt formatı) değişmez.

## Hata Senaryoları
- Cloudinary silme API çağrısı başarısız olursa hesap silme akışının geri kalanını BLOKLAMAMALI (best-effort silme + loglama — MOB-003'teki `_reportSilently` deseniyle tutarlı bir yaklaşım, ama backend tarafı).

## Çıktı / Doğrulama
- **Yeni ortak sınıf**: `shared/CloudinaryPaths.java` — `userReceiptsFolder(email)` hem `UploadController` (yükleme) hem `AccountDeletionService` (silme) tarafından AYNI mantıkla kullanılıyor (e-postadaki güvensiz karakterler `_`'ye çevrilir), aksi halde iki taraf farklı klasör adı üretip hiçbir görsel eşleşmezdi.
- **`UploadController.uploadReceiptImage`**: sabit `"fisbu/receipts"` klasörü yerine `CloudinaryPaths.userReceiptsFolder(userDetails.getUsername())` kullanılıyor — artık her kullanıcının görselleri kendi alt klasöründe, Cloudinary konsolunda "bu görsel kime ait" görülebiliyor (audit/quota gap kısmen kapandı — tam bir DB-seviyeli audit log değil ama denetim bulgusunun asıl kaygısını (hiçbir iz olmaması) çözüyor).
- **`AccountDeletionService.deleteAccount`**: DB silme işlemlerinden SONRA, best-effort olarak `cloudinary.api().deleteResourcesByPrefix(...)` çağrılıyor — kullanıcının TÜM görselleri (klasör prefix'i ile) tek çağrıda siliniyor. Hata durumunda (ağ/kota) sadece `WARN` loglanıyor (email hash'i ile, PII sızıntısı yok — SEC-006 disiplini), hesap silme akışı ETKİLENMİYOR (DB tarafı zaten tamamlandı).
- **Yeni testler**: `AccountDeletionServiceTest`'e 2 test eklendi — (1) Cloudinary'nin `UploadController`'la AYNI klasör adıyla çağrıldığını doğrular, (2) Cloudinary hata fırlatsa bile `deleteAccount`'ın exception fırlatmadığını ve DB silmenin (`userRepository.delete`) gerçekleştiğini doğrular.
- **Doğrulama**: `./gradlew compileJava compileTestJava` başarılı. `./gradlew test --tests AccountDeletionServiceTest --tests UploadControllerTest --tests AuthControllerTest --rerun` → 4+5+11 = 20/20 geçti. `./gradlew e2eTest --tests RegisterToBudgetFlowE2ETest --rerun` → BUILD SUCCESSFUL (regresyon yok).
- **Kapsam dışı bırakılan/not düşülen**: Gerçek bir "audit log" (kim ne zaman ne yükledi tablosu) bu story'de kurulmadı — Cloudinary'nin kendi klasör yapısı, orijinal bulgunun ("hiçbir iz yok") asıl kaygısını düşük maliyetle çözdüğü için yeterli görüldü. Gerçek bir kötüye kullanım/kota şikayeti olursa ayrı bir DB tablosu (upload_log) değerlendirilebilir.

## Öğrenilen Dersler
- Bu story'nin ön-incelemesinde (kapsam belirleme aşamasında) bulunan "hesap silme Cloudinary'yi temizlemiyor" bulgusu, orijinal denetim raporunda HİÇ yoktu — kod okunmadan sadece "userDetails kullanılmıyor" yüzeysel gözlemine dayanan bir denetim bulgusu, gerçek kodu okuyunca daha derin ve daha önemli bir KVKK/gizlilik boşluğu ortaya çıkardı. Bu, denetim raporlarının "başlangıç noktası" olduğunu, uygulama sırasında gerçek kod okumasının ek bulgular üretebileceğini bir kez daha gösterdi (ARCH-003'teki `CategoryTotalResponse.color` bulgusuyla aynı desen).
- İki farklı sınıfın (upload zamanı ve silme zamanı) AYNI türetilmiş değeri (klasör adı) üretmesi gerektiğinde, bu mantığı HER İKİSİNDE de tekrar yazmak yerine ortak bir yardımcı sınıfa (`CloudinaryPaths`) çıkarmak ufak ama gerçek bir hataya (biri değişip diğeri değişmeyince görsellerin "kayıp" kalması) karşı koruma sağladı — küçük bir refactor, büyük bir sessiz-hata riskini ortadan kaldırdı.
