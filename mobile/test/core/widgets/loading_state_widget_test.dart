import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:fisbu/core/widgets/loading_state_widget.dart';

void main() {
  testWidgets('LoadingStateWidget bir CircularProgressIndicator gosterir', (tester) async {
    await tester.pumpWidget(const MaterialApp(home: Scaffold(body: LoadingStateWidget())));

    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  });
}
