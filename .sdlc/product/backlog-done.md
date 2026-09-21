| ID | Başlık | Öncelik | Tahmin | Durum | Sprint |
|---|---|---|---|---|---|
| SEC-001 | Eski pakette (Household, SavingsGoal, Budget, Auth/User controller'ları) her `{id}` alan endpoint için ownership/IDOR kontrolünü doğrula | Acil — güvenlik denetimi bulgusu, kod değişikliği gerektirmeyen bir tarama/rapor işi | S | Done | 1 |
| SEC-003 | `AuthService.java` için ownership/regresyon testleri yaz (kod değiştirmeden önce mevcut davranışı kilitle) | Yüksek — güvenlik denetimi bulgusu | S | Done | 1 |
| SEC-004 | Deploy gate'inin (CI yeşil olmadan deploy engelleniyor mu) Railway/GitHub ayarlarından doğrulanması | Yüksek — güvenlik denetimi bulgusu, repo dışı bir ayar; agent'lar tespit edip raporlar, kendileri değiştirmez | S | Done | 1 |
| SEC-002 | Migration disiplini kurulumu — Flyway ekle, mevcut şemayı baseline migration olarak snapshot'la, `ddl-auto`'yu `validate`'e çevir | Acil — güvenlik denetimi bulgusu, migration disiplini eksikliği veri tutarlılığı riski taşıyor | M | Done | 1 |
| ARCH-001 | Receipt↔Budget döngüsel bağımlılığı + LoadOwnedCategoryPort tekrarı düzeltmesi | Acil — Sprint 2 denetim bulgusu A1 | M | Done | 2 |
| TEST-001 | StatisticsService + ReceiptAiService regresyon testleri | Acil — Sprint 2 denetim bulgusu A5 | M | Done | 2 |
| TEST-002 | StatementImportService regresyon testleri | Acil — Sprint 2 denetim bulgusu A5 | S | Done | 2 |
| TEST-003 | Auth/Budget/Household controller testleri (3/10, kalan TEST-004'te) | Acil — Sprint 2 denetim bulgusu A5 | L | Done | 2 |
| PERF-002 | Scheduler index eksikliği (return_deadline/warranty_expiry_date) + N+1 düzeltmesi | Acil — Sprint 2 denetim bulgusu A4 | S | Done | 2 |
| PERF-001 | Idempotency-Key mekanizması (SavingsGoal) | Acil — Sprint 2 denetim bulgusu A3 | M | Done | 2 |
| TEST-004 | Kalan 7 controller için test yaz (SavingsGoal/StatementImport/Statistics/Upload/Inflation/AI/User) | Acil — Sprint 2 denetim bulgusu A5 | M | Done | 2 |
| SEC-005 | Bilinen CVE'li 2 bağımlılığı yükselt (postgresql, poi-ooxml) | Yüksek | S | Done | - |
| SEC-006 | Log çıktılarında kullanıcı e-postası (PII) sızıntısı düzeltmesi | Orta | S | Done | - |
| E2E-001 | Backend (Testcontainers) + mobil (integration_test) E2E stratejisi, örnek testler, agent kuralları | Yüksek — kullanıcı talebi, gerçek bir production kasma bulgusu ortaya çıkardı | L | Done | - |
| PERF-004 | "Debug modda kasma/donma" araştırması — kök neden: main()'in bildirim izni diyaloğunu runApp()'dan önce bloklaması | Yüksek — gerçek kullanıcı şikayeti, kod satırı seviyesinde kanıtlandı | - | Done (araştırma) | - |
| PERF-005 | PERF-004'ün düzeltmesi — bildirim izni artık runApp()'dan sonra tetikleniyor, açılış 33+s'den 5.4s'ye düştü (canlı emulator'de kanıtlandı) | Yüksek | S | Done | - |
| PERF-006 | ~24s login gecikmesi araştırması — DÜZELTİLDİ: gerçek kök neden `pumpAndSettle`'ın indeterminate spinner'a duyarlılığından kaynaklanan ölçüm artefaktı; gerçek gecikme ~1.2s, gerçek bir performans sorunu yok | Orta | - | Done (araştırma, düzeltilmiş sonuç) | - |
| PERF-007 | (PERF-006'nın düzeltilmiş sonucu nedeniyle Kapatıldı — dayandığı öncül geçersiz, secure-storage yazma işlemi hiçbir zaman yavaş değildi) | - | - | Kapatıldı (gerçek sorun değil) | - |
| MOB-001 | PrivacyInfo.xcprivacy ekle (Apple zorunlu privacy manifest) | Acil — mobil denetim bulgusu A1 | S | Done | - |
| MOB-002 | widget_test.dart'ı düzelt + minimum unit/widget test seti kur + mobile-dev.md'ye kalıcı test kuralı eklendi | Acil — mobil denetim bulgusu A2 | M | Done | - |
| MOB-003 | auth_service.dart'taki boş catch bloklarını düzelt (Crashlytics'e raporlama eklendi) | Acil — mobil denetim bulgusu A3 | S | Done | - |
| MOB-004 | Dashboard'a hata durumu (error state) eklendi | Yüksek — mobil denetim bulgusu Y1 | S | Done | - |
| MOB-005 | Dashboard'daki bağımsız network çağrıları paralelleştirildi (MOB-004 ile birlikte uygulandı) | Yüksek — mobil denetim bulgusu Y2 | S | Done | - |
| ARCH-007 | API versiyonlama — ADR-003, backend `/api/v1` context-path + mobil baseUrl güncellendi, additive-only kuralı backend-dev.md'ye eklendi | Acil (App Store öncesi) — Sprint 2 denetim bulgusu A2 | L | Done | 3 |
| ARCH-002 | AuthService God Service — ADR-004, 4 domain out-port + AccountDeletionService ile hesap silme orkestrasyonu ayrıldı | Yüksek — Sprint 2 denetim bulgusu Y1 | L | Done | 3 |
| ARCH-003 | HouseholdService/StatisticsService receipt port bypass — ADR-005, 4 yeni domain-model port + HouseholdServiceTest (yeni) | Yüksek — Sprint 2 denetim bulgusu Y2 | M | Done | 3 |
| ARCH-004 | StatementImportService bellek riski değerlendirmesi — SONUÇ: gerçek risk yok, 10MB multipart limiti zaten var, kod değişikliği yapılmadı, koruma testi eklendi | Yüksek — Sprint 2 denetim bulgusu Y3 | M | Done (değerlendirme) | 3 |
| ARCH-006 | Pagination — kapsam 6 metoddan 2'ye daraltıldı: kategori-fiş unlink bulk UPDATE'e çevrildi, kategori önerisi 50 kayıtla sınırlandı; backend-dev.md'ye @Modifying/@Transactional kuralı eklendi | Orta-Yüksek — Sprint 2 denetim bulgusu Y7 | S-M | Done | 3 |
| ARCH-005 | UploadController: kullanıcı başına Cloudinary klasörü (CloudinaryPaths) + hesap silmede best-effort toplu Cloudinary temizliği | Orta — Sprint 2 denetim bulgusu Y5, kapsam ön-incelemede genişletildi | S | Done | 3 |
| MOB-006 | flutter analyze temizliği — withOpacity→withValues (101), use_build_context_synchronously→context.mounted (4), unused_import/field (2). 110→3 | Orta — mobil denetim bulgusu O2 | S | Done | - |
| MOB-008 | notification_settings_screen.dart sonsuz loading spinner bug'ı düzeltildi — try/catch + NetworkError.friendlyMessage + Tekrar Dene | Acil — UI/UX denetimi Faz 1, bulgu A1 | S | Done | - |
| MOB-009 | Auth akışının 5 ekranı (login/register/forgot/reset/verify) + CodeInput widget'ı + KVKK sheet AppColors'a bağlandı — gerçek emulator'da açık/koyu tema kanıtlandı | Acil — UI/UX denetimi Faz 5, bulgu A4 | M | Done | - |
| MOB-010 | ErrorStateWidget/EmptyStateWidget/LoadingStateWidget oluşturuldu (henüz ekranlara uygulanmadı — Faz 3'e bırakıldı) — ErrorStateWidget API'si ham exception yerine Object alıp NetworkError.friendlyMessage'i kendi içinde çağırıyor | Orta — UI/UX denetimi Faz 2, bulgu O6 | S | Done | - |
| MOB-011 | MOB-010'un 3 paylaşılan widget'ı 9 dosyaya rollout edildi (budget/categories/household/savings_goals/receipt_list/add_receipt/import_review/financial_chat/profile) — tüm ham `$e` mesajları NetworkError.friendlyMessage'e çevrildi | Orta — UI/UX denetimi Faz 3, bulgu O6 | M | Done | - |
| MOB-012 | statistics_screen.dart (106 hardcoded renk) + spending_personality_screen.dart AppColors'a bağlandı — gerçek emulator'da açık/koyu tema kanıtlandı | Acil — UI/UX denetimi Faz 4, bulgu A3 | L | Done | - |
| MOB-013 | Dağınık sabit renk kalıntıları düzeltildi: dashboard/ocr/categories/household ekranları AppColors token'larına bağlandı; receipt_detail_screen zaten düzeltilmişti (değişiklik yapılmadı) | Yüksek — UI/UX denetimi Faz 6, bulgu A5 | S | Done | - |
| MOB-014 | pin_entry_screen.dart paylaşılan CodeInput'a (yeni obscureText modu ile) geçirildi; add_receipt/receipt_list/pin_entry'deki context'siz AppColors.textSecondary çağrıları düzeltildi | Orta — UI/UX denetimi Faz 7, bulgu O5/O7 | S | Done | - |
| MOB-015 | Cila turu 1/6: export sonrası iptal bildirimi, 3 dosyada küçük dokunma alanı düzeltmesi (48dp), 2 grafik font boyutu büyütüldü | Düşük — UI/UX denetimi, düşük öncelikli bulgular | S | Done | - |
| MOB-016 | Cila turu 2/6: spending_calendar heat-map'e renk-körü erişilebilirlik ipucu (nokta boyutu) eklendi, saf fonksiyona çıkarılıp unit test ile kanıtlandı | Orta — UI/UX denetimi, bulgu O8 | S | Done | - |
| MOB-017 | Cila turu 3/6: dashboard'da "Fiş Ekle" FAB'a taşındı, "Tüm Fişler" tam genişlik kart oldu — gerçek emulator'da açık/koyu tema kanıtlandı | Orta — UI/UX denetimi, bulgu O3 | S | Done | - |
| MOB-018 | Cila turu 4/6: 4 auth ekranındaki tekrarlayan _buildInput fonksiyonu ortak AuthTextField bileşenine çıkarıldı, widget testiyle kanıtlandı | Orta — UI/UX denetimi, bulgu O4 | M | Done | - |
| MOB-019 | Cila turu 5/6: register/reset_password'daki sıralı SnackBar doğrulaması satır-içi hataya (AuthTextField.errorText) çevrildi | Orta — UI/UX denetimi, bulgu O1 | M | Done | - |
| MOB-020 | Cila turu 6/6 (son): receipt_verification akışına adım-geri gitme eklendi, kategori önerisinin geri dönüşte kullanıcı seçimini ezmesi engellendi, widget testiyle kanıtlandı | Orta — UI/UX denetimi, düşük öncelikli bulgu | S | Done | - |
| MOB-023 | Estetik tur 1-4: PressableScale (kart basılı tutma), dashboard sayı animasyonu, LoadingStateWidget shimmer, EmptyStateWidget illüstrasyonu — gerçek emulator'da kanıtlandı | Orta — App Store hazırlık sonrası estetik tur | M | Done | - |
| MOB-024 | Renk/görsel profesyonellik denetimi 1→2→5→4→6: AppColors.cardShadow standardı (15 kart), statistics/budget pozitif-negatif renk semantiği birleştirildi, auth ekranlarında tekrarlanan hex/ikon/border-radius temizlendi | Yüksek-Orta — App Store hazırlık sonrası görsel denetim | M | Done | - |
| MOB-026 | Privacy Manifest'e NSPrivacyAccessedAPICategoryFileTimestamp (C617.1) eklendi; Privacy Policy yayını GitHub Pages admin yetkisi gerektirdiği için kullanıcıya devredildi | Acil — App Store submission bloklayıcıları | S | Done (Pages kullanıcı bekliyor) | - |
