import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:fisbu/core/widgets/loading_state_widget.dart';

void main() {
  testWidgets('LoadingStateWidget birden fazla shimmer yer tutucu gosterir', (tester) async {
    await tester.pumpWidget(const MaterialApp(home: Scaffold(body: LoadingStateWidget())));
    await tester.pump();

    expect(find.byType(ShaderMask), findsNWidgets(4));
  });

  testWidgets('LoadingStateWidget animasyonu tekrarlar (deterministic pump ile crash etmeden ilerler)',
      (tester) async {
    await tester.pumpWidget(const MaterialApp(home: Scaffold(body: LoadingStateWidget())));

    await tester.pump(const Duration(milliseconds: 400));
    await tester.pump(const Duration(milliseconds: 1200));
    await tester.pump(const Duration(milliseconds: 1200));

    expect(find.byType(ShaderMask), findsNWidgets(4));
  });

  testWidgets('LoadingStateWidget dispose edilince AnimationController temizlenir (hata firlatmaz)',
      (tester) async {
    await tester.pumpWidget(const MaterialApp(home: Scaffold(body: LoadingStateWidget())));
    await tester.pump();

    await tester.pumpWidget(const MaterialApp(home: Scaffold(body: SizedBox())));
    await tester.pumpAndSettle();
  });
}
