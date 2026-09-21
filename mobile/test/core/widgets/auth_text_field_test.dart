import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:fisbu/core/widgets/auth_text_field.dart';

void main() {
  testWidgets('AuthTextField label ve ikonu gosterir', (tester) async {
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: AuthTextField(
          controller: TextEditingController(),
          label: 'E-posta',
          icon: Icons.email_outlined,
        ),
      ),
    ));

    expect(find.text('E-posta'), findsOneWidget);
    expect(find.byIcon(Icons.email_outlined), findsOneWidget);
  });

  testWidgets('obscureText true iken TextField gizli modda render olur', (tester) async {
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: AuthTextField(
          controller: TextEditingController(),
          label: 'Şifre',
          icon: Icons.lock_outline,
          obscureText: true,
        ),
      ),
    ));

    final field = tester.widget<TextField>(find.byType(TextField));
    expect(field.obscureText, isTrue);
  });

  testWidgets('suffixIcon verildiginde render edilir ve tiklanabilir', (tester) async {
    var tapped = false;
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: AuthTextField(
          controller: TextEditingController(),
          label: 'Şifre',
          icon: Icons.lock_outline,
          suffixIcon: IconButton(
            icon: const Icon(Icons.visibility_outlined),
            onPressed: () => tapped = true,
          ),
        ),
      ),
    ));

    await tester.tap(find.byIcon(Icons.visibility_outlined));
    expect(tapped, isTrue);
  });

  testWidgets('yazilan metin controller\'a yansir', (tester) async {
    final controller = TextEditingController();
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: AuthTextField(
          controller: controller,
          label: 'E-posta',
          icon: Icons.email_outlined,
        ),
      ),
    ));

    await tester.enterText(find.byType(TextField), 'test@fisbu.com');
    expect(controller.text, 'test@fisbu.com');
  });
}
