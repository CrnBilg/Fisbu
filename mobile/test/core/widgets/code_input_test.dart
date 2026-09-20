import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:fisbu/core/widgets/code_input.dart';

bool _hasCircleIndicator(WidgetTester tester) {
  final containers = tester.widgetList<Container>(find.byType(Container));
  return containers.any((c) {
    final decoration = c.decoration;
    return decoration is BoxDecoration && decoration.shape == BoxShape.circle;
  });
}

void main() {
  testWidgets('CodeInput obscureText=false iken girilen rakami metin olarak gosterir', (tester) async {
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(body: CodeInput(length: 4, onChanged: (_) {})),
    ));

    await tester.enterText(find.byType(TextField).first, '7');
    await tester.pump();

    expect(find.text('7'), findsWidgets);
    expect(_hasCircleIndicator(tester), isFalse);
  });

  testWidgets('CodeInput obscureText=true iken rakam yerine nokta/daire gosterir', (tester) async {
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(body: CodeInput(length: 4, obscureText: true, onChanged: (_) {})),
    ));

    await tester.enterText(find.byType(TextField).first, '7');
    await tester.pump();

    expect(_hasCircleIndicator(tester), isTrue);
  });

  testWidgets('CodeInput onChanged tum kutulardaki degeri birlestirip cagirir', (tester) async {
    String? result;
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(body: CodeInput(length: 3, onChanged: (v) => result = v)),
    ));

    final fields = find.byType(TextField);
    await tester.enterText(fields.at(0), '1');
    await tester.pump();
    await tester.enterText(fields.at(1), '2');
    await tester.pump();

    expect(result, '12');
  });
}
