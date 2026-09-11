**Durum**: Done
**Tahmin**: S

## Kaynak
Sprint 2 backend denetimi — StatementImportService (229 satır, sıfır test), finansal ekstre içe aktarma.

## Write-set
- `backend/src/test/java/com/fisbu/api/service/StatementImportServiceTest.java` (yeni)

## Kabul Kriterleri
SEC-003 deseniyle tutarlı: mevcut davranış kilitlenir, kod değiştirilmez.

## Çıktı / Doğrulama / Öğrenilen Dersler

### Çıktı
`StatementImportServiceTest.java` eklendi — 16 test, `parseStatement()` public metodunu
uçtan uca kapsıyor (private yardımcı metotlar dolaylı olarak kapsanıyor):
- Genel dosya doğrulama: null dosya, boş dosya, desteklenmeyen uzantı, uzantısız dosya adı (4 test)
- Uygulamanın kendi export CSV formatı (`tryParseAppCsv`): başarılı deterministik parse +
  kategori eşleştirme, UTF-8 BOM'lu dosya, `TOPLAM` satırının atlanması, geçersiz tutarlı
  satırın atlanması, kullanıcı bulunamazsa 404 (5 test)
- Bilinmeyen formatlı CSV → AI fallback: AI ile parse, AI hiç işlem bulamazsa uyarı,
  8000 karakter üstü metnin kırpılıp uyarı eklenmesi (3 test)
- PDF akışı: magic-byte yoksa 400, magic-byte var ama gerçek PDF değilse 422, metin içeren
  gerçek bir PDF (PDFBox ile üretilip test içinde bellekte oluşturuldu) AI'ya gönderiliyor,
  metinsiz PDF 422 (4 test)
- Mockito + AssertJ, SEC-003/`AuthServiceTest`'teki üslup ve adlandırma deseniyle tutarlı
  (`metot_senaryo_beklenenSonuc`), `@ExtendWith(MockitoExtension.class)`.
- Production kodu (`StatementImportService.java`) HİÇ değiştirilmedi.

### Doğrulama
Sadece hedefli komut çalıştırıldı (canlı Supabase DB'sine bağlanan `ApiApplicationTests`
tetiklenmedi):
```
./gradlew test --tests "com.fisbu.api.service.StatementImportServiceTest"
BUILD SUCCESSFUL in 5s
```
JUnit XML raporu (`build/test-results/test/TEST-com.fisbu.api.service.StatementImportServiceTest.xml`)
teyidi: `tests="16" skipped="0" failures="0" errors="0"`, toplam süre 2.481s.

### Öğrenilen Dersler
- Gerçek PDF parsing davranışını (magic-byte kontrolü + PDFBox metin çıkarma) mock'lamak
  yerine PDFBox'ın kendisiyle bellekte küçük bir PDF üretip test etmek, hem gerçek davranışı
  kilitler hem de dışarıdan ikili (binary) fixture dosyası eklemeyi gerektirmez.
- Servis legacy stilde doğrudan `CategoryRepository`/`UserRepository` kullanıyor (repository
  soyutlaması yok) — SEC-003'teki `AuthServiceTest` deseniyle aynı şekilde bu repository'ler
  doğrudan mock'landı.
- `parseStatement` iki farklı davranış ailesine ayrılıyor: uygulamanın kendi CSV formatı için
  deterministik (AI'sız) parse, diğer her şey için AI fallback — bu ayrımı doğrulayan testler
  (`AiCagirmaz` / `AiIleParseEder`) regresyonu doğrudan yakalayacak en kritik testler.
- `MAX_AI_TEXT_LENGTH` (8000 karakter) kırpma davranışı ince bir detaydı; testte tam olarak
  8000 karakter uzunluğunda metin gönderildiği `argThat` ile doğrulandı.
