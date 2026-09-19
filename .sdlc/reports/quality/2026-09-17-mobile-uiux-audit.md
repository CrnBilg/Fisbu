# Mobil UI/UX Denetim Raporu

**Tarih**: 2026-09-17
**Kapsam**: 27 ekran, 8 boyut (tutarlılık, geri bildirim, boş durumlar, hata mesajları, navigasyon, görsel hiyerarşi, erişilebilirlik, mikro-etkileşimler)
**Yöntem**: 4 paralel salt-okunur denetim (auth akışı / fiş-finans akışı / bütçe-kategori-aile-import / istatistik-profil-ayarlar), her ekran gerçek kod okunarak. Ekran görüntüsü alınamadı (çalışan emulator yok), tamamen kod analizi. Hiçbir dosya değiştirilmedi.

---

## ACİL / YÜKSEK — App Store öncesi en çok etkileyen bulgular

### A1. `notification_settings_screen.dart` — gerçek bir bug, kozmetik değil
**Dosya**: `lib/screens/notification_settings_screen.dart:23-31` (`_loadPrefs()`)
İlk yükleme isteğinde **hiç `try/catch` yok**. Sunucu isteği başarısız olursa `_isLoading` asla `false` olmuyor — kullanıcı **sonsuza kadar loading spinner'da kilitli kalıyor**, hata mesajı veya "tekrar dene" seçeneği yok. Bu diğer bulguların aksine bir UX-polish meselesi değil, gerçek bir kullanılamazlık senaryosu (ağ hatası olan her kullanıcı bu ekranı hiç kullanamaz).

### A2. Ham exception mesajları kullanıcıya sızıyor — 9+ dosyada tekrarlayan desen
Proje zaten `NetworkError.friendlyMessage` adında bir çözüm barındırıyor (bazı ekranlarda kullanılıyor) ama tutarsız uygulanmış. Aşağıdaki noktalarda `'$e'`/`e.toString()` doğrudan SnackBar/Text'e basılıyor — kullanıcı "Exception: SocketException: Failed host lookup" gibi teknik/İngilizce metin görebilir:
- `add_receipt_screen.dart:349` (`Hata: $e`)
- `receipt_list_screen.dart:409` (`Silinemedi: $e`)
- `budget_screen.dart:109,127`
- `categories_screen.dart:93,121`
- `household_screen.dart:115,135,170`
- `savings_goals_screen.dart:115,136,162,348`
- `import_review_screen.dart:161` — **en kritik an**: kullanıcı AI'ın çıkardığı işlemleri onaylarken
- `financial_chat_screen.dart:61-62`
- `profile_screen.dart:199,644`

**Neden en öncelikli**: En yaygın (9 dosya, 15+ konum), en kolay düzeltilebilir (mevcut `NetworkError.friendlyMessage` yardımcısını çağırmak yeterli), ve App Store review'da doğrudan görülebilir bir kalite sinyali.

### A3. `statistics_screen.dart` + `spending_personality_screen.dart` — tema sistemi tamamen dışında
**Dosya**: `lib/screens/statistics_screen.dart` (1852 satır) — **106 adet hardcoded `Color(0xFF...)`**, kendi eski mor/koyu paletini kullanıyor (`0xFF6C63FF`, `0xFF1A1A2E`, `0xFF9E9EBF`), `AppColors.primary` (indigo `0xFF6366F1`) ile hiç örtüşmüyor. `spending_personality_screen.dart:58` aynı eski gradient'i tekrarlıyor. Bu iki ekran, uygulamanın geri kalanına (calendar, chat, notification_settings — hepsi doğru `AppColors` kullanıyor) hiç benzemiyor; sanki farklı bir uygulamadan kopyalanmış gibi duruyor.

### A4. Auth akışının 5 ekranı tema sistemini tamamen atlıyor
**Dosyalar**: `login_screen.dart`, `register_screen.dart`, `forgot_password_screen.dart`, `reset_password_screen.dart`, `verify_email_screen.dart` — hepsi sabit koyu gradyan (`Color(0xFF0F0F1A)`→`Color(0xFF1A1A2E)`) kullanıyor, `AppColors`/`AppTheme`'i hiç okumuyor. **Kullanıcı sistem temasını açık seçse bile bu 5 ekran zorla koyu kalıyor.** `pin_entry_screen.dart` ve `auth_wrapper.dart` ise doğru şekilde context-duyarlı `AppColors` kullanıyor — aynı akış içinde tutarsızlık var.

### A5. Diğer sabit/kalıntı renkler (küçük ama tekrarlayan desen)
- `dashboard_screen.dart:283,297,340` — `AppColors`'da zaten karşılığı olan tonlar (`categoryUlasim`, `categoryMarket`) sabit hex ile tekrar tanımlanmış.
- `receipt_detail_screen.dart:230-233` — context'siz sabit gradient, dark modda dashboard'daki gibi uyum sağlamıyor.
- `ocr_screen.dart:300-301` — elle yazılmış, karmaşık dark-mode alpha hesaplaması (tema sisteminin amacını atlıyor).
- `categories_screen.dart:14,16` — kategori renk seçenekleri `AppColors` dışında sabit hex.
- `household_screen.dart:345` — `AppColors.primary`'ye yakın ama farklı bir mor gradient, kalıntı görünümünde.

---

## ORTA — belirgin ama daha az kritik

### O1. Register formunda hata deneyimi zayıf
**Dosya**: `register_screen.dart:118-169` — 6 farklı doğrulama kuralı (KVKK, boş alan, format, uzunluk vb.) tek tek ayrı SnackBar ile gösteriliyor, alan bazlı satır-içi hata yok. Kullanıcı formu defalarca gönderip her seferinde bir sonraki hatayı "sırayla" öğreniyor.

### O2. Ekstre import hata mesajları yetersiz açıklayıcı
**Dosya**: `import_statement_screen.dart:40-42` — PDF/CSV parse edilemediğinde kullanıcıya "neden" (şifreli PDF mi, tanınmayan format mı, boş dosya mı) konusunda somut rehberlik verilmiyor. Bu akış, en çok kafa karışıklığı/destek talebi doğurma riski taşıyan nokta.

### O3. Dashboard'da görsel hiyerarşi zayıf
**Dosya**: `dashboard_screen.dart:245-350` — 6-7 eylem kartı ("Fiş Ekle", "Tüm Fişler", "İstatistikler", "Fişi Tara", "Bütçe", "Rapor Al", "Ekstre İçe Aktar") hepsi aynı boyut/ağırlıkta alt alta sıralanmış. En kritik aksiyon ("Fiş Ekle") öne çıkmıyor, FAB da yok.

### O4. Kod tekrarı — `_buildInput` 4 auth ekranında birebir kopya
`login_screen.dart`, `register_screen.dart`, `forgot_password_screen.dart`, `reset_password_screen.dart` — aynı ~50 satırlık input-builder fonksiyonu 4 yerde ayrı ayrı tanımlanmış. `lib/core/widgets/` altında paylaşılan bir `AuthTextField` yok. Bakım riski: bir yerde stil değişirse 4 yerde ayrı güncelleme gerekiyor.

### O5. Tutarsız kod girişi deneyimi
`pin_entry_screen.dart:119-135` — segment'li `CodeInput` widget'ı (diğer ekranlarda kullanılan) yerine düz `TextField` kullanılmış. Aynı üründe iki farklı "kod girme" görsel dili var.

### O6. Ortak empty-state/error-state/loading bileşeni yok
`lib/core/widgets/` altında sadece `category_picker.dart`, `code_input.dart`, `offline_banner.dart` var. Her ekran kendi boş/hata/yükleniyor UI'sini tekrar yazıyor — küçük tutarsızlıklar (ikon var/yok, CTA var/yok, renk semantiği) buradan birikiyor. Örnek: `budget_screen.dart:164-189` kategori boşken CTA yok, ama `categories_screen.dart:167-204` ve `household_screen.dart` boş durumlarında CTA var.

### O7. Context-siz `AppColors` çağrıları (dark modda donuk kalma riski)
`add_receipt_screen.dart:498,644`, `receipt_list_screen.dart:180,279-280,504-506,526` — `AppColors.textSecondary` (sabit) ile `AppColors.txt(context)`/`txtSecondary(context)` (context-duyarlı) aynı dosyada karışık kullanılmış; sabit olanlar dark modda okunmaz kalabilir.

### O8. Grafik/harita renk ayrımı erişilebilirlik riski
`spending_calendar_screen.dart:69-73` (`_heatColor`) — harcama yoğunluğu tek rengin (indigo) SADECE opaklığıyla gösteriliyor; renk körü kullanıcılar için düşük/yüksek günler arasındaki farkı ayırt etmek zor olabilir (ikinci bir görsel ipucu yok).

---

## DÜŞÜK — kozmetik, ölçülü öncelik

- **Mikro-etkileşim eksikliği (uygulama geneli)**: `statistics_screen.dart`, `spending_personality_screen.dart`, `financial_chat_screen.dart`, `dashboard_screen.dart` — hiçbir yerde `AnimationController`/`TweenAnimationBuilder`/`AnimatedSwitcher` yok, grafikler/kartlar/mesajlar aniden beliriyor. Uygulama genelinde tutarlı bir "eksiklik" olduğu için, tek bir ekranı düzeltmek yerine bütüncül ele alınmalı.
- **Erişilebilirlik — küçük dokunma alanları**: `pin_entry_screen.dart:141-154` (buton ~44-46pt, sınırda), `add_receipt_screen.dart:717-721` (satır silme ikonu `isDense` satırda), `split_bill_screen.dart:297-302` (kişi silme ikonu).
- **Grafik font boyutları küçük**: `statistics_screen.dart:1622,1208` — eksen/etiket 10-11px.
- **Haptic feedback hiçbir yerde yok** (silme, kaydetme, hata gibi aksiyonlarda) — tüm uygulamada tutarlı bir eksiklik, tek ekran sorunu değil.
- **Export sonrası sessiz geçiş**: `export_screen.dart:94-97` — paylaşım sheet'i direkt açılıyor, SnackBar/onay yok; paylaşım iptal edilirse kullanıcı sonucu bilmiyor.
- **Adım-geri gidememe**: `receipt_verification_screen.dart` — çok adımlı doğrulama akışında önceki adıma dönme yok (sadece ileri).

---

## Olumlu Bulgular (referans/örnek alınabilir)
- `receipt_verification_screen.dart` — `LinearProgressIndicator` ile net adım göstergesi, iyi navigasyon örneği.
- `spending_calendar_screen.dart` — tamamen `AppColors` kullanan, `NetworkError.friendlyMessage` ile tutarlı hata yöneten, grup içindeki en iyi ekran.
- `export_screen.dart` — hata mesajı yönetiminde grup içi en tutarlı ekran.
- `notification_settings_screen.dart` toggle akışı — optimistic update + rollback + SnackBar deseni iyi (sadece ilk yüklemedeki try/catch eksikliği A1'de ayrı bir bug).
- `receipt_list_screen.dart` silme akışı — onay diyaloğu + swipe + SnackBar iyi bir UX örneği.
- `import_review_screen.dart` — confidence-score renk kodlaması ve seçili satır vurgusu güçlü bir görsel hiyerarşi örneği.
- `pin_entry_screen.dart`, `auth_wrapper.dart` — auth grubunda tema sistemini doğru kullanan tek ekranlar.

---

## Önerilen Öncelik Sırası (uygulama fazı için)
1. **A1** — notification_settings_screen sonsuz spinner bug'ı (gerçek kullanılamazlık)
2. **A2** — ham hata mesajları → `NetworkError.friendlyMessage` (9 dosya, tek tip düzeltme, yüksek etki/düşük efor)
3. **A4** — auth ekranlarının tema sistemine bağlanması (5 ekran, App Store'da en görünür tutarsızlık)
4. **A3** — statistics_screen/spending_personality_screen'in `AppColors`'a geçirilmesi (en büyük dosya, en çok hardcoded renk — muhtemelen en büyük efor)
5. **A5** — dağınık küçük sabit renk kalıntıları (dashboard/receipt_detail/ocr/categories/household)
6. **O1-O8** — orta öncelikliler, kullanıcıdan sıralama onayı alındıktan sonra
7. **Düşük öncelikliler** — kozmetik, ayrı bir "cila" turu olarak ele alınabilir
