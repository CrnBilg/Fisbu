// E2E-001: "login -> fiş oluştur -> kategoriye ata -> bütçe listesinde görünüyor mu"
// akışının gerçek bir simulator/emulator üzerinde, gerçek widget'larla sürülen uçtan
// uca testi.
//
// KAPSAM NOTU: "kayıt ol" adımı burada UI üzerinden sürülmüyor — mobil uygulama e-posta
// doğrulama kodunu girecek bir ekrana sahip (verify_email_screen.dart) ama kodun kendisi
// e-posta kutusuna gittiği için otomasyonda okunamıyor. Bu adım backend E2E'de
// (RegisterToBudgetFlowE2ETest, backend/src/e2e/) gerçek HTTP + gerçek DB ile kapsandı.
// Bu test, doğrulanmış bir kullanıcıyı (test hazırlığında API ile önceden oluşturulmuş)
// kullanarak login'den başlar — mobil E2E'nin katma değeri asıl burada: gerçek widget
// ağacı, gerçek navigasyon, gerçek network round-trip.
//
// ÇALIŞTIRMA: gerçek bir backend'e ihtiyaç var (ör. yerel bir Postgres + `bootRun`,
// production Supabase'e ASLA bağlanılmaz). Örnek:
//   flutter test integration_test/receipt_to_budget_flow_test.dart -d <device> \
//     --dart-define=API_BASE_URL=http://localhost:8090 \
//     --dart-define=E2E_EMAIL=... --dart-define=E2E_PASSWORD=... \
//     --dart-define=E2E_CATEGORY_NAME=...
//
// PERF GÖZLEMİ (kullanıcı bildirimi: debug modda kasma/donma): her kritik adımın süresi
// ölçülüp yazdırılır. Bir adım [SLOW_STEP_THRESHOLD]'i aşarsa test BAŞARISIZ OLMAZ (bu
// bir gözlem/smoke testi, katı bir SLA testi değil) ama konsola açıkça işaretlenir —
// insan gözden geçirmesi ve backlog'a PERF-XXX olarak not düşülmesi için.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

import 'package:fisbu/main.dart' as app;

const Duration _slowStepThreshold = Duration(milliseconds: 2500);

const String _email = String.fromEnvironment('E2E_EMAIL');
const String _password = String.fromEnvironment('E2E_PASSWORD');
const String _categoryName = String.fromEnvironment('E2E_CATEGORY_NAME', defaultValue: 'E2E Mobil Market');

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  final Map<String, Duration> stepTimings = {};

  Future<void> timedStep(WidgetTester tester, String label, Future<void> Function() action) async {
    final stopwatch = Stopwatch()..start();
    await action();
    stopwatch.stop();
    stepTimings[label] = stopwatch.elapsed;
    final marker = stopwatch.elapsed > _slowStepThreshold ? '[E2E-PERF][ANORMAL GECİKME]' : '[E2E-PERF]';
    debugPrint('$marker "$label" adımı ${stopwatch.elapsedMilliseconds} ms sürdü');
  }

  testWidgets('login -> fiş ekle -> kategoriye ata -> bütçede görünüyor', (tester) async {
    expect(_email.isNotEmpty, isTrue, reason: 'E2E_EMAIL --dart-define ile verilmeli');
    expect(_password.isNotEmpty, isTrue, reason: 'E2E_PASSWORD --dart-define ile verilmeli');

    await timedStep(tester, 'Uygulama açılışı (main + ilk frame)', () async {
      app.main();
      // NOT: PERF-004 araştırması sırasında, bildirim izni ÖNCEDEN verilmemişse
      // (native GrantPermissionsActivity) bu adımın süresiz bloklandığı bulundu —
      // bkz. .sdlc/stories/PERF-004.md. Bu testin çalışması için izin test kurulumunda
      // önceden verilir (`adb shell pm grant ... POST_NOTIFICATIONS`).
      for (int i = 0; i < 15; i++) {
        await tester.pump(const Duration(seconds: 1));
        if (find.text('Tekrar\nHoşgeldin').evaluate().isNotEmpty) break;
      }
      await tester.pumpAndSettle(const Duration(seconds: 3));
    });

    // ---------- Login ----------
    if (find.text('Tekrar\nHoşgeldin').evaluate().isEmpty) {
      final visibleTexts = tester.widgetList<Text>(find.byType(Text))
          .map((w) => w.data)
          .where((t) => t != null && t.trim().isNotEmpty)
          .toSet();
      debugPrint('[E2E-DEBUG] Login ekranı bulunamadı, ekrandaki metinler: $visibleTexts');
    }
    expect(find.text('Tekrar\nHoşgeldin'), findsOneWidget, reason: 'Login ekranı açılmadı');

    await tester.enterText(find.byType(TextField).first, _email);
    await tester.enterText(find.byType(TextField).at(1), _password);
    await tester.pumpAndSettle();

    await timedStep(tester, 'Login (POST /auth/login + Dashboard geçişi)', () async {
      await tester.tap(find.text('Giriş Yap'));
      await tester.pumpAndSettle(const Duration(seconds: 8));
    });

    expect(find.text('Fiş Ekle'), findsOneWidget, reason: 'Dashboard açılmadı — login başarısız olabilir');

    // ---------- Fiş Ekle ----------
    await timedStep(tester, 'AddReceiptScreen açılışı', () async {
      await tester.tap(find.text('Fiş Ekle'));
      await tester.pumpAndSettle(const Duration(seconds: 5));
    });

    expect(find.text('Mağaza Adı'), findsOneWidget);

    // Not: TextField'ların labelText'i decoration içinde olduğu için doğrudan metinle
    // eşleşmez — index bazlı erişim daha güvenilir (Mağaza Adı = ilk TextField).
    await tester.enterText(find.byType(TextField).at(0), 'E2E Mobil Migros');
    await tester.enterText(find.byType(TextField).at(1), '275.25');
    await tester.pumpAndSettle();

    // Tarih seç -> Material date picker (varsayılan initialDate: bugün) -> OK
    await tester.tap(find.text('Tarih seç'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('OK'));
    await tester.pumpAndSettle();

    // Kategori seç -> bottom sheet -> önceden oluşturulmuş kategoriye dokun
    await tester.tap(find.text('Kategori seç'));
    await tester.pumpAndSettle();
    expect(find.text(_categoryName), findsOneWidget,
        reason: 'Test hazırlığında oluşturulan kategori ($_categoryName) listede yok');
    await tester.tap(find.text(_categoryName));
    await tester.pumpAndSettle();

    await timedStep(tester, 'Fiş kaydet (POST /receipts + navigasyon)', () async {
      await tester.tap(find.text('Kaydet'));
      await tester.pumpAndSettle(const Duration(seconds: 8));
    });

    // ---------- Bütçe ekranında yansımayı doğrula ----------
    expect(find.text('Fiş Ekle'), findsOneWidget, reason: 'Fiş kaydedildikten sonra Dashboard\'a dönülmedi');

    await timedStep(tester, 'BudgetScreen açılışı (GET /budgets)', () async {
      await tester.tap(find.text('Bütçe'));
      await tester.pumpAndSettle(const Duration(seconds: 8));
    });

    expect(find.text('Bütçelerim'), findsOneWidget);
    expect(find.text(_categoryName), findsWidgets, reason: 'Bütçe listesinde kategori görünmüyor');
    // Kritik doğrulama: fişin tutarı (275,25 TL) bütçenin currentSpend'ine yansımış olmalı
    expect(find.textContaining('275,25'), findsOneWidget,
        reason: 'Fişin tutarı bütçe ekranında yansımıyor — receipt->budget entegrasyonu (ARCH-001) bozulmuş olabilir');

    debugPrint('[E2E] Tam mobil akış başarılı. Adım süreleri: $stepTimings');
  });
}
