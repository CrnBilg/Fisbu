import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:fisbu/core/widgets/empty_state_widget.dart';

void main() {
  Widget wrap(Widget child) => MaterialApp(home: Scaffold(body: child));

  group('EmptyStateWidget', () {
    testWidgets('sadece ikon ve baslik verildiginde bunlari gosterir, '
        'subtitle/CTA yoksa gostermez', (tester) async {
      await tester.pumpWidget(wrap(const EmptyStateWidget(
        icon: Icons.category_outlined,
        title: 'Henüz kategori yok',
      )));

      expect(find.byIcon(Icons.category_outlined), findsOneWidget);
      expect(find.text('Henüz kategori yok'), findsOneWidget);
      expect(find.byType(ElevatedButton), findsNothing);
    });

    testWidgets('subtitle verildiginde gosterir', (tester) async {
      await tester.pumpWidget(wrap(const EmptyStateWidget(
        icon: Icons.category_outlined,
        title: 'Henüz kategori yok',
        subtitle: '+ butonuna basarak kategori ekle',
      )));

      expect(find.text('+ butonuna basarak kategori ekle'), findsOneWidget);
    });

    testWidgets('ctaLabel + onCtaPressed ikisi de verildiginde CTA butonu gorunur ve basilinca tetiklenir',
        (tester) async {
      var tapped = false;
      await tester.pumpWidget(wrap(EmptyStateWidget(
        icon: Icons.category_outlined,
        title: 'Henüz kategori yok',
        ctaLabel: 'Kategori Ekle',
        onCtaPressed: () => tapped = true,
      )));

      expect(find.widgetWithText(ElevatedButton, 'Kategori Ekle'), findsOneWidget);
      await tester.tap(find.text('Kategori Ekle'));
      await tester.pump();

      expect(tapped, isTrue);
    });

    testWidgets('sadece ctaLabel verilip onCtaPressed verilmezse CTA butonu GOSTERILMEZ '
        '(yarim/tikanamayan bir buton olusmamali)', (tester) async {
      await tester.pumpWidget(wrap(const EmptyStateWidget(
        icon: Icons.category_outlined,
        title: 'Henüz kategori yok',
        ctaLabel: 'Kategori Ekle',
      )));

      expect(find.byType(ElevatedButton), findsNothing);
    });
  });
}
