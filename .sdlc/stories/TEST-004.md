**Durum**: Done
**Tahmin**: M

## Kaynak
Sprint 2 backend denetimi — TEST-003'ün kapsamadığı kalan 7 controller: SavingsGoal, StatementImport, Statistics, Upload, Inflation, AI, User.

## Write-set
- `backend/src/test/java/com/fisbu/api/controller/*Test.java` (yeni, `@WebMvcTest` ile — DB bağlantısı gerektirmeden)

## Kabul Kriterleri
TEST-003'teki desenle tutarlı: `@WebMvcTest` + gerçek `SecurityConfig` (`@Import`), controller'ın service bağımlılıkları `@MockitoBean`. Her endpoint için mutlu yol + yetkisiz istekte 403.

## Çıktı
7 yeni test dosyası eklendi (production kodu hiç değiştirilmedi):
- `SavingsGoalControllerTest.java` (10 test)
- `StatementImportControllerTest.java` (5 test)
- `StatisticsControllerTest.java` (10 test)
- `UploadControllerTest.java` (5 test) — bir subagent'ın çalışması bitmeden durması (600s timeout) nedeniyle PDM tarafından tamamlandı; Cloudinary `Uploader`'ı mock'layarak gerçek magic-byte kontrolünü (bildirilen content-type'a değil dosya baytlarına güvenme) doğruluyor. Sprint 2 denetiminin bulduğu "userDetails parametresi kullanılmıyor" durumu bilinçli olarak değiştirilmedi, mevcut davranış olduğu gibi test edildi.
- `InflationControllerTest.java` (4 test)
- `AiControllerTest.java` (8 test)
- `UserControllerTest.java` (10 test) — PDM tarafından tamamlandı (bkz. Upload notu).

TEST-003'teki 3 controller (Auth/Budget/Household, 33 test) ile birlikte proje artık **10/10 controller'da test kapsamına** sahip.

## Doğrulama
`./gradlew test --tests "com.fisbu.api.controller.*"` → BUILD SUCCESSFUL.

JUnit XML raporları (tümü `failures="0" errors="0"`):
| Controller | Test sayısı |
|---|---|
| Auth | 11 |
| Budget | 11 |
| Household | 11 |
| SavingsGoal | 10 |
| StatementImport | 5 |
| Statistics | 10 |
| Upload | 5 |
| Inflation | 4 |
| AI | 8 |
| User | 10 |
| **TOPLAM** | **85** |

Canlı Supabase DB'sine hiç bağlanılmadı (`ApiApplicationTests`/argümansız `test` çalıştırılmadı).

Genel karar: **Geçti.** Puan: **9/10** (Test kapsamı: 10 — tüm controller'lar artık kapsanıyor; Kod kalitesi: 9 — desen tutarlı, `@Import(SecurityConfig.class)` ile gerçek yetkilendirme zinciri test ediliyor; Güvenlik: 8 — Upload'daki bilinen audit-gap'i test aşamasında değiştirmedi, sadece belgeledi) → Onay.

## İnceleme
7 dosyanın 5'i bir subagent tarafından üretildi ve PDM tarafından derleme+test ile teyit edildi; kalan 2'si (Upload, User) subagent 600 saniye ilerlemesizlik nedeniyle durunca PDM tarafından aynı desenle tamamlandı. Tüm dosyalar tek bir toplu komutla (85 test) yeniden doğrulandı. **Karar: Onay.**

## Öğrenilen Dersler
- Bir subagent'ın uzun bir görevde (7 dosya, art arda derleme+test döngüleri) "ilerlemesizlik" nedeniyle durması, işin TAMAMEN kaybolması anlamına gelmiyor — dosya sistemi kalıcı olduğu için, PDM `git status`/`find` ile "nereye kadar gelinmiş" diye kontrol edip kalan kısmı tamamlayabildi. Ders: bir agent durduğunda önce ne üretmiş olduğunu kontrol et, sıfırdan başlatma.
- `Cloudinary`/`Uploader` sınıflarının Mockito ile (final sınıf olmalarına rağmen) sorunsuz mock'lanabildiği görüldü — Spring Boot 3.5 ile gelen Mockito sürümü inline mock maker'ı varsayılan kullanıyor.
- Upload endpoint'indeki bilinen audit bulgusunu (userDetails kullanılmıyor) testte "beklenen davranış" olarak doğrulamak yerine, story dosyasına açıkça not düşüldü — testin bunu "onaylıyormuş" gibi görünmemesi için.
