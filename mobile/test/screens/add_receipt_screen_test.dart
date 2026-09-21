import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:fisbu/screens/add_receipt_screen.dart';

void main() {
  group('AddReceiptScreen duzenleme modu', () {
    testWidgets('editingReceiptId verildiginde baslik "Fişi Düzenle" olur', (tester) async {
      await tester.pumpWidget(const MaterialApp(
        home: AddReceiptScreen(editingReceiptId: 42, initialStoreName: 'Migros'),
      ));
      await tester.pump();

      expect(find.text('Fişi Düzenle'), findsOneWidget);
      expect(find.text('Fiş Ekle'), findsNothing);
    });

    testWidgets('editingReceiptId verilmediginde baslik "Fiş Ekle" olur', (tester) async {
      await tester.pumpWidget(const MaterialApp(home: AddReceiptScreen()));
      await tester.pump();

      expect(find.text('Fiş Ekle'), findsOneWidget);
    });

    testWidgets('duzenleme modunda fotograf ve urunler bolumu gizlenir', (tester) async {
      await tester.pumpWidget(const MaterialApp(
        home: AddReceiptScreen(editingReceiptId: 42, initialStoreName: 'Migros'),
      ));
      await tester.pump();

      expect(find.text('Fiş Fotoğrafı Ekle'), findsNothing);
      expect(find.text('Ürünler (opsiyonel)'), findsNothing);
    });

    testWidgets('normal modda fotograf ve urunler bolumu gorunur', (tester) async {
      await tester.pumpWidget(const MaterialApp(home: AddReceiptScreen()));
      await tester.pump();

      expect(find.text('Fiş Fotoğrafı Ekle'), findsOneWidget);
      expect(find.text('Ürünler (opsiyonel)'), findsOneWidget);
    });

    testWidgets('duzenleme modunda alanlar initial degerlerle dolu gelir', (tester) async {
      await tester.pumpWidget(const MaterialApp(
        home: AddReceiptScreen(
          editingReceiptId: 42,
          initialStoreName: 'Migros',
          initialAmount: '150.00',
        ),
      ));
      await tester.pump();

      expect(find.widgetWithText(TextField, 'Migros'), findsOneWidget);
      expect(find.widgetWithText(TextField, '150.00'), findsOneWidget);
    });
  });
}
