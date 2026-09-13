// PERF-006 doğrulama: "ilk login mi yavaş, yoksa HER login mi yavaş?" sorusunu
// cevaplamak için AuthService.login()'u aynı uygulama kurulumunda (reinstall
// olmadan, Keystore alias'ı korunarak) iki kez arka arkaya çağırıp süresini ölçer.
// Geçici bir teşhis testidir — production kodu değiştirmez.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

import 'package:fisbu/services/auth_service.dart';

const String _email = String.fromEnvironment('E2E_EMAIL');
const String _password = String.fromEnvironment('E2E_PASSWORD');

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('PERF-006: ilk login vs ikinci login süresi', (tester) async {
    expect(_email.isNotEmpty, isTrue, reason: 'E2E_EMAIL --dart-define ile verilmeli');

    final sw1 = Stopwatch()..start();
    final result1 = await AuthService.login(_email, _password);
    sw1.stop();
    debugPrint('[PERF-006] İlk login: ${sw1.elapsedMilliseconds} ms, başarı=${result1.success}');
    expect(result1.success, isTrue, reason: 'İlk login başarısız: ${result1.errorMessage}');

    await AuthService.logout();

    final sw2 = Stopwatch()..start();
    final result2 = await AuthService.login(_email, _password);
    sw2.stop();
    debugPrint('[PERF-006] İkinci login (aynı kurulum, reinstall YOK): ${sw2.elapsedMilliseconds} ms, başarı=${result2.success}');
    expect(result2.success, isTrue, reason: 'İkinci login başarısız: ${result2.errorMessage}');

    await AuthService.logout();

    final sw3 = Stopwatch()..start();
    final result3 = await AuthService.login(_email, _password);
    sw3.stop();
    debugPrint('[PERF-006] Üçüncü login (aynı kurulum): ${sw3.elapsedMilliseconds} ms, başarı=${result3.success}');

    debugPrint('[PERF-006] SONUÇ: 1.=${sw1.elapsedMilliseconds}ms, 2.=${sw2.elapsedMilliseconds}ms, 3.=${sw3.elapsedMilliseconds}ms');
  });
}
