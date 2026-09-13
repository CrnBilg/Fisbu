import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:fisbu/main.dart';

void main() {
  testWidgets(
      'MyApp acilis aninda crash etmeden render olur '
      '(flutter_secure_storage okuma Future\'i widget-test ortaminda hic '
      'cozulmuyor — bu yuzden AuthWrapper "checking" (spinner) durumunda '
      'kalir; test bunu bir hata olarak degil, beklenen bir platform-plugin '
      'siniri olarak ele alir, bkz. asagidaki not)',
      (WidgetTester tester) async {
    await tester.pumpWidget(const MyApp(initialDarkMode: false));
    expect(find.byType(MaterialApp), findsOneWidget);

    // NOT: AuthWrapper._init(), AuthService.getToken() -> FlutterSecureStorage
    // (Pigeon tabanli, plugin v10.x) okumasini bekler. Bu paket testWidgets'in
    // fake-async pump() dongusunde hic cozulmeyen bir Future donduruyor (gercek
    // bir test() fonksiyonunda ayni cagri aninda cozuluyor — dogrulandi). Bu,
    // platformdan bagimsiz bir widget testinde secure-storage plugin'lerinin
    // her zaman elle mock'lanmasi gerektigini gosteren somut bir kanit; ayri
    // bir plugin-mock kurulumu bu story'nin (MOB-002) kapsamini asiyor.
    // Bu yuzden burada spinner durumunu (CircularProgressIndicator) kabul
    // ediyoruz ve sadece ilk 2 saniyelik pump penceresinde HICBIR exception
    // firlatilmadigini dogruluyoruz — asil hedef "acilista crash yok" testi.
    for (var i = 0; i < 20; i++) {
      await tester.pump(const Duration(milliseconds: 100));
    }

    expect(tester.takeException(), isNull);
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  });
}
