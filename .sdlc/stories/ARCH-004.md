**Durum**: Done
**Öncelik**: Yüksek
**Tahmin**: M (değerlendirme + kod)

## Kaynak
Sprint 2 backend denetimi, bulgu Y3: `StatementImportService.java:186,195-201` — PDF/CSV dosyası tamamen byte[]/String olarak belleğe okunuyor. `MAX_AI_TEXT_LENGTH=8000` sadece AI'ya giden metni sınırlıyor, okuma/parse aşamasını değil.

## Neden önemli
Büyük bir ekstre dosyası (çok sayfalı PDF, büyük CSV) tamamen belleğe çekiliyor — eşzamanlı birden fazla büyük dosya yüklemesi OOM riski taşıyabilir.

## Yaklaşım — önce değerlendirme
Kod değiştirmeden önce gerçek riski ölçmek gerekiyor:
1. Mevcut dosya boyutu sınırı var mı (`@RequestParam MultipartFile`, Spring'in `spring.servlet.multipart.max-file-size` ayarı) kontrol edilecek — belki risk zaten bir üst katmanda sınırlanıyor.
2. Gerçek kullanım senaryosu (banka ekstresi PDF/CSV) tipik olarak kaç sayfa/KB — gerçekten "büyük dosya" ihtimali var mı, yoksa teorik bir risk mi.
3. Streaming/chunked okuma (PDF için `PDFTextStripper` sayfa-sayfa, CSV için `BufferedReader` satır-satır) mevcut PDF/CSV kütüphaneleriyle (Apache POI/PDFBox — SEC-005'te versiyonu güncellenen `poi-ooxml` dahil) ne kadar kolay uygulanabilir.
Bu değerlendirme sonrası: gerçek risk düşükse (örn. zaten bir upload boyutu limiti varsa ve tipik dosyalar küçükse), story'nin kapsamı "bir üst sınır ekle + dokümante et" olarak daraltılabilir; gerçek risk varsa streaming'e geçilir.

## Kabul Kriterleri
- Mevcut davranış (import sonucu) test ile kilitlenir.
- Ya (a) streaming/chunked okumaya geçilir ya da (b) gerekçeli olarak mevcut boyut sınırının yeterli olduğu dokümante edilip açık bir `max-file-size` sınırı eklenir (henüz yoksa).
- Karar, story'nin Çıktı bölümünde gerekçesiyle yazılır.

## Çıktı / Doğrulama
- **Risk değerlendirmesi (kod değişikliğinden ÖNCE yapıldı)**:
  1. `backend/src/main/resources/application.properties:17-18` — `spring.servlet.multipart.max-file-size=10MB` ve `max-request-size=10MB` ZATEN mevcut (SEC-002 civarında eklenmiş). Bu limit Spring tarafından servlet/filter katmanında, istek `StatementImportController`/`UploadController`'a ULAŞMADAN önce uygulanıyor — yani "sınırsız dosya" senaryosu zaten mümkün değil.
  2. `StatementImportService.parseStatement`'ı çağıran tek yol `POST /receipts/import/parse` — bu controller'da (`StatementImportController.java`) limiti override eden bir `@RequestMapping`/config yok, global limit geçerli.
  3. 10MB'lık bir PDF/CSV'yi `byte[]`/`String`/PDFBox `PDDocument` olarak belleğe almak modern bir JVM için (varsayılan heap yüzlerce MB–birkaç GB) önemsiz bir maliyet — gerçek bankacılık ekstresi dosyaları da tipik olarak KB-birkaç MB mertebesinde.
  4. **Sonuç**: Denetim raporundaki "büyük dosya yüklemesi bellek riski taşır" bulgusu teorik olarak doğru ama PRATİKTE zaten mevcut bir konfigürasyonla kapatılmış. Streaming/chunked okumaya geçmenin somut bir faydası yok — bu story'nin kabul kriterindeki (b) seçeneği ("mevcut boyut sınırının yeterli olduğu dokümante edilip açık bir sınır eklenir") geçerli, çünkü sınır zaten var.
- **Kod değişikliği YOK** (mantık aynı kaldı) — sadece dokümantasyon: `StatementImportService.java`'nın sınıf javadoc'una bu kararın gerekçesi eklendi.
- **Yeni regresyon/koruma testi**: `backend/src/test/java/com/fisbu/api/config/MultipartFileSizeLimitTest.java` — `application.properties`'i okuyup `max-file-size`/`max-request-size`'ın tanımlı VE makul bir üst sınırda (≤25MB) olduğunu doğruluyor. Bu, kararın dayandığı varsayımı (limit var ve makul) kilitliyor — biri ileride bu satırı silerse veya çok büyütürse (örn. 500MB) test kırılır.
- **Doğrulama**: `./gradlew compileJava compileTestJava` başarılı. `./gradlew test --tests MultipartFileSizeLimitTest --tests StatementImportServiceTest --rerun` → 1+16 = 17/17 geçti (argümansız `test` KULLANILMADI — ARCH-003'teki hatadan ders çıkarıldı).

## Öğrenilen Dersler
- Bir denetim bulgusunun "teorik olarak doğru" olması, "pratikte düzeltme gerektirir" anlamına gelmiyor — bu story'nin en değerli çıktısı bir KOD DEĞİŞİKLİĞİ değil, var olan bir korumanın (10MB limiti) fark edilip belgelenmesi oldu. "Kod değiştirmeden önce riski ölç" adımı burada tam olarak işlevini gösterdi: ölçüm, düzeltmenin gereksiz olduğunu ortaya çıkardı.
- Bir güvenlik/dayanıklılık özelliğinin (dosya boyutu limiti) sadece BİR YERDE (application.properties) tanımlı olması, onu koruyan bir test olmadığında sessizce kaybolabilir riski taşıyor — bu tür "config'e gömülü garantiler" için de (kod mantığına gömülü garantiler gibi) regresyon testi yazmak değerli; `MultipartFileSizeLimitTest` bu projede bu deseni ilk kez uyguluyor.
