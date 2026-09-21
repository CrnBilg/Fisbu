import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:fisbu/core/theme/app_page_transition.dart';
import 'package:fisbu/core/theme/app_theme.dart';

void main() {
  testWidgets('AppTheme android ve iOS icin ayni fade+slide gecisini kullanir', (tester) async {
    final theme = AppTheme.light;
    final builders = theme.pageTransitionsTheme.builders;

    expect(builders[TargetPlatform.android], isA<AppFadeSlidePageTransitionsBuilder>());
    expect(builders[TargetPlatform.iOS], isA<AppFadeSlidePageTransitionsBuilder>());
  });

  testWidgets('sayfa gecisi hata firlatmadan render olur', (tester) async {
    await tester.pumpWidget(MaterialApp(
      theme: AppTheme.light,
      home: Builder(
        builder: (context) => Scaffold(
          body: Center(
            child: ElevatedButton(
              onPressed: () => Navigator.push(
                context,
                MaterialPageRoute(builder: (context) => const Scaffold(body: Text('ikinci sayfa'))),
              ),
              child: const Text('git'),
            ),
          ),
        ),
      ),
    ));

    await tester.tap(find.text('git'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 150));
    await tester.pumpAndSettle();

    expect(find.text('ikinci sayfa'), findsOneWidget);
  });
}
