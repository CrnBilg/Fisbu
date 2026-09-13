// PERF-006 düzeltilmiş ölçüm: pumpAndSettle(8s) kullanmadan, kısa aralıklı pump()
// döngüsüyle "Dashboard gerçekte ne zaman görünür oldu" sorusunu cevaplar —
// pumpAndSettle'ın sürekli bir animasyona (CircularProgressIndicator gibi)
// duyarlılığından kaynaklanan yanıltıcı ölçümü ortadan kaldırır.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

import 'package:fisbu/main.dart' as app;

const String _email = String.fromEnvironment('E2E_EMAIL');
const String _password = String.fromEnvironment('E2E_PASSWORD');

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('PERF-006: gerçek wall-clock UI görünme süresi', (tester) async {
    app.main();
    for (int i = 0; i < 15; i++) {
      await tester.pump(const Duration(milliseconds: 500));
      if (find.text('Tekrar\nHoşgeldin').evaluate().isNotEmpty) break;
    }
    expect(find.text('Tekrar\nHoşgeldin'), findsOneWidget);

    await tester.enterText(find.byType(TextField).first, _email);
    await tester.enterText(find.byType(TextField).at(1), _password);
    await tester.pump(const Duration(milliseconds: 300));

    final sw = Stopwatch()..start();
    await tester.tap(find.text('Giriş Yap'));

    int elapsedAtDashboard = -1;
    for (int i = 0; i < 60; i++) {
      await tester.pump(const Duration(milliseconds: 500));
      if (find.text('Fiş Ekle').evaluate().isNotEmpty) {
        elapsedAtDashboard = sw.elapsedMilliseconds;
        break;
      }
    }
    sw.stop();

    debugPrint('[PERF-006-UI] Giriş Yap tıklamasından Dashboard\'ın (Fiş Ekle metni) '
        'GERÇEKTEN görünür olduğu ana kadar: $elapsedAtDashboard ms');
    expect(elapsedAtDashboard, greaterThan(-1), reason: 'Dashboard 30 saniye içinde görünmedi');

    // Dashboard göründükten sonra hâlâ bir CircularProgressIndicator dönüyor mu
    // (veri yükleniyor mu) kontrol et — pumpAndSettle'ın yanıltıcı olup olmadığını anlamak için
    final stillLoading = find.byType(CircularProgressIndicator).evaluate().isNotEmpty;
    debugPrint('[PERF-006-UI] Dashboard göründüğü anda hâlâ CircularProgressIndicator var mı: $stillLoading');
  });
}
