import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:fisbu/core/widgets/success_check_overlay.dart';

void main() {
  testWidgets('gosterilince check ikonunu render eder ve otomatik kapanir', (tester) async {
    await tester.pumpWidget(MaterialApp(
      home: Builder(
        builder: (context) => Scaffold(
          body: Center(
            child: ElevatedButton(
              onPressed: () => SuccessCheckOverlay.show(context, message: 'Kaydedildi'),
              child: const Text('tetikle'),
            ),
          ),
        ),
      ),
    ));

    await tester.tap(find.text('tetikle'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 50));

    expect(find.byIcon(Icons.check_rounded), findsOneWidget);
    expect(find.text('Kaydedildi'), findsOneWidget);

    // Otomatik kapanma suresini (650ms) + gecis animasyonlarini gecince
    // overlay'in kapandigini dogrula
    await tester.pump(const Duration(milliseconds: 700));
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.check_rounded), findsNothing);
  });
}
