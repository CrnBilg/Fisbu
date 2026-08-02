# Marka Logoları

Buraya, `mobile/lib/core/utils/brand_helper.dart` içindeki `_logos` haritasında
tanımlı dosya adlarıyla eşleşen PNG logo dosyalarını ekle (örn. `ebebek.png`,
`a101.png`). Öneriler:

- Transparan arka plan, kare formata yakın (örn. 128×128 veya 256×256 px)
- Dosya adı haritadaki `value` alanındaki ada birebir uymalı

Bir dosya henüz eklenmemişse veya bulunamıyorsa uygulama otomatik olarak
kategori ikonuna düşer (bkz. `core/widgets/brand_avatar.dart`) — yani eksik
dosya hiçbir zaman hata/kırık görsel oluşturmaz, ekleyince anında devreye girer.

Yeni bir marka eklemek istersen: logo dosyasını buraya koy + `brand_helper.dart`
içindeki `_logos` haritasına bir satır ekle, başka hiçbir değişiklik gerekmez.
