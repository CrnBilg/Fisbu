# ADR-004: AuthService God Service Sorununun Çözümü

**Durum**: Kabul edildi
**Tarih**: 2026-09-13
**İlgili story**: ARCH-002
**Kaynak**: Sprint 2 backend denetimi, bulgu Y1 — `AuthService.java:23-27,45-49,277-288`

## Bağlam
`AuthService` (legacy `com.fisbu.api.service` paketinde), kimlik doğrulama/kayıt/şifre/e-posta doğrulama sorumluluklarının yanında `CategoryRepository`, `ReceiptRepository`, `BudgetRepository`, `SavingsGoalRepository`'yi doğrudan enjekte ediyor ve bunları yalnızca `deleteAccount()` metodunda kullanıyor (satır 277-288):

```java
budgetRepository.deleteAll(budgetRepository.findByUser(user));
savingsGoalRepository.deleteAll(savingsGoalRepository.findByUser(user));
receiptRepository.deleteAll(receiptRepository.findByUser(user));
categoryRepository.deleteAll(categoryRepository.findByUser(user));
userRepository.delete(user);
```

Burada iki ayrı sorun iç içe geçmiş:
1. **Sorumluluk ihlali (God Service)**: `AuthService`, kimlik doğrulama dışında 4 farklı domain'in veri yaşam döngüsünü yönetiyor.
2. **Domain sınırı ihlali**: `receipt`, `category`, `budget` modülleri zaten hexagonal'a taşınmış ve kendi `application/port/out/` port'larını tanımlamışken (bkz. ADR-001, `SumReceiptSpendByCategoryPort`, `LoadOwnedCategoryPort`), `AuthService` bu port'ları atlayıp repository'lere doğrudan erişiyor.

Ek karmaşıklık: `SavingsGoalRepository` henüz hexagonal'a taşınmamış (`com.fisbu.api.repository` legacy paketinde, `application/port` katmanı yok). Bu ADR, `SavingsGoal`'ün tam hexagonal migrasyonunu kapsam dışı bırakır (bu, ayrı bir ARCH story'si olmalı) — sadece AuthService'in ona doğrudan erişimini bir port arkasına alır.

Silme sırası önemli: Budget, category'ye NOT NULL FK ile bağlı olduğu için önce budget/savingsGoal/receipt, sonra category, en son user silinmeli. Bu, ADR-001'deki receipt→budget senaryosundan farklı olarak **sıralamaya bağımlı, tek transaction içinde tamamlanması gereken bir iş akışı** — event-driven (fire-and-forget dinleyiciler) bu garantiyi doğal olarak vermiyor.

## Değerlendirilen Seçenekler

### Seçenek 1: Domain event ile silme (`UserAccountDeletionRequestedEvent` + her modülün kendi `@EventListener`'ı)
ADR-001'deki desenle birebir tutarlı: `AuthService`, event yayınlar; `receipt`, `category`, `budget`, `savingsGoal` kendi listener'larında temizliği yapar.
Artı: ADR-001 ile aynı desen, ekstra altyapı gerekmez (senkron `@EventListener`).
Eksi: Silme sırası (budget/savingsGoal → receipt → category → user, FK nedeniyle) event dinleyicileri arasında **zımni bir sıralamaya** dayanmak zorunda kalır — Spring'in `@EventListener`'ları `@Order` ile sıralanabilir olsa da, bu "gizli" bir bağımlılık yaratır ve modüller arası sıralama varsayımını okunabilir kod yerine framework anotasyonuna gömer. Ayrıca kullanıcı silme (`userRepository.delete(user)`) event yayınlayanın kendisinde kalmalı, bu da "her modül kendi işini yapar" temiz ayrımını bir miktar bozar (son adım hâlâ AuthService/orchestrator'da). Hataya daha açık: bir listener sessizce atlanırsa (ör. exception yutulursa) hesap yarım silinmiş kalabilir ve bunu fark etmek zorlaşır.

### Seçenek 2: Her domain kendi "delete-user-data" out-port'unu tanımlar, yeni bir `AccountDeletionService` bunları doğrudan (senkron, sıralı) orkestre eder — SEÇİLDİ
`receipt`, `category`, `budget` modüllerine sırasıyla `DeleteReceiptsByUserPort`, `DeleteCategoriesByUserPort`, `DeleteBudgetsByUserPort` out-port'ları eklenir (mevcut `application/port/out/` katmanına, `SumReceiptSpendByCategoryPort` ile aynı yerde). `SavingsGoal` için de aynı desende bir port tanımlanır, ancak modül henüz hexagonal'a taşınmadığı için port arayüzü geçici olarak `com.fisbu.api.shared.application.port.out.DeleteSavingsGoalsByUserPort` altında (paylaşılan/nötr bir pakette) tanımlanır ve implementasyonu legacy `SavingsGoalRepository`'yi sarmalar — `SavingsGoal` tam hexagonal'a taşındığında bu port kendi modülüne (`savingsgoal/application/port/out/`) taşınır (takip eden ayrı bir ARCH story'si açılacak).
`AuthService`'ten `deleteAccount()` çıkarılır, yeni bir `AccountDeletionService` (auth/hesap yönetimiyle ilgili ama kendi tek sorumluluğu "hesap silme" olan ayrı bir sınıf) bu 4 port'u + `UserRepository`'yi enjekte eder ve FK sırasına göre (budget → savingsGoal → receipt → category → user) tek `@Transactional` metod içinde çağırır.
Artı: Sıralama açık ve okunabilir kalır (kod içinde sıralı metod çağrıları, gizli event sıralaması yok). Tek transaction garantisi korunur (mevcut davranışla birebir aynı). Domain sınırları port'lar üzerinden korunur — hiçbir modül repository'sine dıştan doğrudan erişim yok. AuthService artık sadece kimlik doğrulama sorumluluğunu taşır (Single Responsibility). SavingsGoal'ün henüz hexagonal olmaması, bu ADR'nin kapsamını genişletmeden (geçici sarmalayıcı port ile) ileriye ertelenir.
Eksi: Yeni bir sınıf (`AccountDeletionService`) ve 4 yeni port arayüzü + adapter eklenir — Seçenek 1'e göre biraz daha fazla dosya. Event-driven deseniyle (ADR-001) birebir aynı mekanizma kullanılmıyor, projede iki farklı "modüller arası iletişim" deseni bir arada bulunacak (event: bildirim/reaksiyon amaçlı yan etkiler için; doğrudan port çağrısı: sıralı/transaction'a bağlı kritik iş akışları için). Bu, gerekçesi açıkça yazılmak kaydıyla kabul edilebilir bir tutarsızlık.

## Karar
**Seçenek 2** uygulanır. Gerekçe: hesap silme, ADR-001'deki "bir domain diğerinin yan etkisini tetikler" senaryosundan farklı olarak, **FK sırasına bağımlı, tek transaction'da atomik olması gereken bir orkestrasyon**dur. Event tabanlı çözüm bu sıralama garantisini gizli/örtük hale getirirken, doğrudan port orkestrasyonu sıralamayı kodda açık ve test edilebilir kılar. Domain event deseni (ADR-001), reaksiyon niteliğindeki (fire-and-react) senaryolar için; doğrudan port çağrısı ise sıralı/kritik-yol iş akışları için kullanılacak — bu ayrım projede bir kural olarak benimsenir.

### Uygulama Planı (backend-dev için)
1. **Yeni out-port'lar** (mevcut `application/port/out/` katmanlarına, ADR-001'deki `SumReceiptSpendByCategoryPort` ile aynı konvansiyonda):
   - `com.fisbu.api.receipt.application.port.out.DeleteReceiptsByUserPort` — `void deleteAllByUser(User user)` (veya `userId`, mevcut port'ların imza konvansiyonuna uyacak şekilde).
   - `com.fisbu.api.category.application.port.out.DeleteCategoriesByUserPort`
   - `com.fisbu.api.budget.application.port.out.DeleteBudgetsByUserPort`
   - `com.fisbu.api.shared.application.port.out.DeleteSavingsGoalsByUserPort` (geçici — SavingsGoal hexagonal'a taşınana kadar `shared`'da kalır, taşınınca `savingsgoal/application/port/out/`'a taşınır).
2. **Adapter'lar**: her port'u ilgili mevcut persistence adapter sınıfında (veya SavingsGoal için yeni küçük bir adapter sınıfında, `legacy repository`'yi sarmalayan) implemente et; implementasyon mevcut `repository.deleteAll(repository.findByUser(user))` mantığını birebir korur (davranış değişmez).
3. **Yeni sınıf**: `com.fisbu.api.service.AccountDeletionService` (veya kimlik/hesap yönetimi use-case katmanı neredeyse oraya) — 4 port + `UserRepository` enjekte edilir, `@Transactional public void deleteAccount(String email)` metodu FK sırasına göre (budget → savingsGoal → receipt → category → user) sırayla çağırır.
4. **AuthService değişikliği**: `deleteAccount()` metodu ve `CategoryRepository`/`ReceiptRepository`/`BudgetRepository`/`SavingsGoalRepository` alanları + constructor parametreleri kaldırılır. `AuthService` sadece `UserRepository`, `PasswordEncoder`, `JwtService`, `EmailService` bağımlılıklarını taşır.
5. **Controller değişikliği**: hesap silme endpoint'ini çağıran controller (`AuthController` veya ilgili controller), `AuthService.deleteAccount(...)` yerine `AccountDeletionService.deleteAccount(...)`'ı çağıracak şekilde güncellenir.
6. **Regresyon testi (ÖNCE, kod değişmeden)**: mevcut `deleteAccount` davranışını (hangi tabloların, hangi sırayla, tam olarak neyin silindiğini) kilitleyen bir entegrasyon/regresyon testi yazılır; refactor sonrası aynı test aynı sonucu doğrulamalı.

## Sonuç
`AuthService` artık yalnızca kimlik doğrulama sorumluluğunu taşır ve hiçbir domain repository'sini doğrudan enjekte etmez. Hesap silme orkestrasyonu `AccountDeletionService`'e taşınır ve her domain'in verisi kendi tanımladığı out-port üzerinden silinir — domain encapsulation'ı korunur. `SavingsGoal` modülünün tam hexagonal migrasyonu bu ADR'nin kapsamı dışında bırakılır ve ayrı bir takip story'si olarak açılması önerilir.
