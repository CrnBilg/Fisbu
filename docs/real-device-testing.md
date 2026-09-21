# Gerçek iOS Cihazında Test Etme (Geliştirme Sertifikasıyla)

Apple Developer Program üyeliği açıldığında (ya da ücretsiz bir kişisel geliştirici hesabıyla, 7 günlük geçici imza ile) FişBu'yu doğrudan kendi iPhone'una yükleyip kamera/OCR akışını test edebilirsin.

## Ön Koşullar
- Mac'te Xcode kurulu olmalı (App Store'dan).
- iPhone, Mac'e USB kablo ile bağlı (veya aynı Wi-Fi ağında, kablosuz geliştirme açıksa).
- iPhone'da **Ayarlar → Gizlilik ve Güvenlik → Geliştirici Modu** açık olmalı (iOS 16+, ilk bağlantıda Xcode bunu otomatik ister).

## Adımlar

1. **Apple ID'ni Xcode'a ekle** (ücretsiz kişisel hesap yeterli, Developer Program üyeliği gerekmez):
   Xcode → Settings (⌘,) → Accounts → "+" → Apple ID ile giriş yap.

2. **Proje ayarlarında imzalama takımını seç**:
   ```
   cd mobile/ios
   open Runner.xcworkspace
   ```
   Xcode açılınca sol panelden **Runner** projesine tıkla → **Signing & Capabilities** sekmesi → **Team** alanından Apple ID'ni seç. "Automatically manage signing" işaretli kalsın.
   > Not: Ücretsiz hesapla imzalanan uygulamalar 7 günde bir yeniden yüklenmeli (geliştirme sertifikası süresi doluyor). Ücretli Developer Program üyeliği bu sınırı kaldırır.

3. **iPhone'u bağla, cihazı seç**: Xcode'un üst çubuğundaki cihaz seçiciden kendi iPhone'unu seç (simulator listesinde değil, "Devices" bölümünde görünür).

4. **Flutter ile doğrudan çalıştır** (Xcode yerine terminalden de olur):
   ```
   cd mobile
   flutter devices          # iPhone'un listede göründüğünü doğrula
   flutter run -d <cihaz-id> --release
   ```
   İlk yüklemede iPhone'da "Güvenilmeyen Geliştirici" uyarısı çıkarsa: **Ayarlar → Genel → VPN ve Cihaz Yönetimi** → geliştirici profilini bul → **Güven**.

5. **Test edilecek akış**: Fişi Tara (kamera) → gerçek bir fiş fotoğrafı çek → OCR/AI restorasyon sonucu ve güven skorunu kontrol et → gerekirse Face ID/Touch ID ile giriş yap.

## Notlar
- `flutter run --release` (debug değil) kullanmak, performans/açılış süresini gerçekçi şekilde ölçmeni sağlar.
- Sorun yaşarsan `flutter run -v` ile ayrıntılı log alabilirsin.
- Bu adımı ben (agent) yapamıyorum — fiziksel donanım ve Apple ID kimlik doğrulaması gerektiriyor.
