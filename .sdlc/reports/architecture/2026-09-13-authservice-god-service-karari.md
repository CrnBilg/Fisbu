# Mimari Karar Özeti: AuthService God Service (ARCH-002)

**Tarih**: 2026-09-13
**ADR**: `.sdlc/adr/ADR-004-authservice-god-service.md`

## Sorun
`AuthService`, kimlik doğrulama sorumluluğunun ötesinde `CategoryRepository`, `ReceiptRepository`, `BudgetRepository`, `SavingsGoalRepository`'yi doğrudan enjekte ediyor ve `deleteAccount()` içinde kullanıyordu — hexagonal domain sınırlarını (receipt/category/budget kendi port'larını tanımlamışken) bypass ediyordu.

## Karar
Her domain (`receipt`, `category`, `budget`) kendi "delete-user-data" out-port'unu tanımlar; `SavingsGoal` için (henüz hexagonal'a taşınmadığından) geçici olarak `shared` paketinde bir port tanımlanır. Yeni bir `AccountDeletionService`, bu 4 port'u FK sırasına göre (budget → savingsGoal → receipt → category → user) tek transaction içinde sırayla çağırır. `AuthService`'ten `deleteAccount()` ve 4 repository bağımlılığı tamamen kaldırılır — AuthService yalnızca kimlik doğrulama sorumluluğunu taşır.

Event tabanlı çözüm (ADR-001'deki desenle aynı) reddedildi çünkü hesap silme, FK sırasına bağımlı ve tek transaction'da atomik olması gereken bir orkestrasyon — event/listener deseni bu sıralama garantisini örtük hale getirir. Doğrudan port çağrısı sıralamayı kodda açık ve test edilebilir bırakır.

## Değişecek Dosyalar (uygulama backend-dev'e bırakıldı)
- Yeni: `receipt/application/port/out/DeleteReceiptsByUserPort.java` + adapter implementasyonu
- Yeni: `category/application/port/out/DeleteCategoriesByUserPort.java` + adapter implementasyonu
- Yeni: `budget/application/port/out/DeleteBudgetsByUserPort.java` + adapter implementasyonu
- Yeni: `shared/application/port/out/DeleteSavingsGoalsByUserPort.java` (geçici, SavingsGoal legacy repository'sini sarmalar)
- Yeni: `com.fisbu.api.service.AccountDeletionService`
- Değişen: `com.fisbu.api.service.AuthService` — `deleteAccount()` ve 4 repository bağımlılığı kaldırılır
- Değişen: hesap silme endpoint'ini çağıran controller — `AccountDeletionService`'e yönlendirilir
- Yeni: refactor ÖNCESİ yazılacak regresyon testi (mevcut silme davranışını kilitler)

## Takip
`SavingsGoal` modülünün tam hexagonal migrasyonu bu ADR kapsamı dışında — ayrı bir ARCH story olarak önerilir.
