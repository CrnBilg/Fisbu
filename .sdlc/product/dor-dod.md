## Definition of Ready (DoR)
Bir iş sprint'e girmeden önce hepsi ✅ olmalı:

- [ ] Kabul kriterleri yazılmış.
- [ ] Eğerki mimari veya tasarım belirsizliği var ise bu belirsizlik çözülmüş olmalı.
- [ ] Küçük ve yönetilebilir olarak oluşturulmuş. Bir sprint içerisinde rahatça tamamlanabilecek büyüklükte olmalıdır.
- [ ] En az 5 tane hata senaryosu yazılmış (kod değişikliği içermeyen tarama/doğrulama işleri hariç — bkz. story'nin kendi notu).
- [ ] İşin hangi dosyalara dokunacağı (write-set) belirtilmiş.
- [ ] Tema (light/dark) etkisi değerlendirilmiş (bkz. `global-ui` skill — proje şu an tek dilli, dil etkisi değerlendirmesi bu nedenle kapsam dışı).


## Definition of Done (DoD)
Bir iş 'bitti' (Done) sayılması için hepsi ✅ olmalı:

- [ ] Tek bir fonksiyon üzerinden Unit test yapılır.
- [ ] Parçalar birlikte çalışırken Integration test yapılır ve testi başarıyla geçer.
- [ ] Arayüz bileşeninin (Flutter widget) doğru davrandığını ölçmek için widget/component test yapılır.
- [ ] Kullanıcı uçtan uca gerçek bir işlemi tamamlayıp tamamlayamadığı, kritik akışlarda E2E/manuel doğrulama ile test edilir.
- [ ] Kod reviewer tarafından incelenmiş (review edilmiş) ve kritik bulgu kalmamış olmalı.
- [ ] Yetkilendirme/kaynak erişimi (ownership) değişikliği içeren story'lerde `security` agent'ı çağrılmış ve IDOR bulgusu kalmamış olmalı.
- [ ] Tema renkleri (açık/koyu) doğrulanmış olmalı (bkz. `global-ui` skill).
- [ ] PDM, story'nin canlı ortamda (gerçek DB + backend + mobil ayağa kaldırılarak) uçtan uca çalıştığını bizzat doğrulamış olmalı — sadece otomatik testlerin geçmesi yeterli değildir.
- [ ] Reviewer, yeni eklenen endpoint'in yetkisiz-erişim davranışının (401/403/404 seçimi, ownership kontrolü), aynı kaynak üzerindeki mevcut endpoint'lerle tutarlı olduğunu kontrol etmiş olmalı.
- [ ] Migration içeren story'lerde devops, Flyway migration'ın uygulandığını VE çalışan sürecin güncel derlemeyi çalıştırdığını doğrulamış olmalı (bkz. `devops.md` — migration/deploy senkronizasyon kuralı).
- [ ] DoR'da yazılan kabul kriterleri ve hata senaryolarının tamamının karşılandığı kontrol edilmiş olmalı.
