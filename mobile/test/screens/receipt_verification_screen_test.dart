import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:fisbu/screens/receipt_verification_screen.dart';
import 'package:fisbu/models/restore_receipt_result.dart';

void main() {
  Widget buildScreen() => MaterialApp(
        home: ReceiptVerificationScreen(
          aiResult: RestoreReceiptResult(confidenceScore: 40),
        ),
      );

  testWidgets('Geri butonu bir onceki adima doner (mağaza <-> tutar)', (tester) async {
    await tester.pumpWidget(buildScreen());
    await tester.pumpAndSettle();

    expect(find.text('Mağaza adı doğru mu?'), findsOneWidget);

    await tester.tap(find.text('Onayla, Devam Et'));
    await tester.pumpAndSettle();
    expect(find.text('Tutar doğru mu?'), findsOneWidget);

    await tester.tap(find.byIcon(Icons.arrow_back));
    await tester.pumpAndSettle();
    expect(find.text('Mağaza adı doğru mu?'), findsOneWidget);
  });

  testWidgets('geri gidip tekrar ileri gidince onceki adimda girilen veri KAYBOLMAZ', (tester) async {
    await tester.pumpWidget(buildScreen());
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextField), 'Migros');
    await tester.tap(find.text('Onayla, Devam Et'));
    await tester.pumpAndSettle();
    expect(find.text('Tutar doğru mu?'), findsOneWidget);

    await tester.tap(find.byIcon(Icons.arrow_back));
    await tester.pumpAndSettle();

    expect(find.widgetWithText(TextField, 'Migros'), findsOneWidget);
  });

  testWidgets('ilk adimda geri butonu ekrandan cikar (Navigator.pop)', (tester) async {
    final navigatorKey = GlobalKey<NavigatorState>();
    await tester.pumpWidget(MaterialApp(
      navigatorKey: navigatorKey,
      home: Builder(
        builder: (context) => Scaffold(
          body: Center(
            child: ElevatedButton(
              onPressed: () => Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => ReceiptVerificationScreen(
                    aiResult: RestoreReceiptResult(confidenceScore: 40),
                  ),
                ),
              ),
              child: const Text('ac'),
            ),
          ),
        ),
      ),
    ));

    await tester.tap(find.text('ac'));
    await tester.pumpAndSettle();
    expect(find.text('Mağaza adı doğru mu?'), findsOneWidget);

    await tester.tap(find.byIcon(Icons.arrow_back));
    await tester.pumpAndSettle();

    expect(find.text('Mağaza adı doğru mu?'), findsNothing);
    expect(find.text('ac'), findsOneWidget);
  });
}
