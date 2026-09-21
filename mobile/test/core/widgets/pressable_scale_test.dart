import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:fisbu/core/widgets/pressable_scale.dart';

void main() {
  testWidgets('parmak basiliyken kuculuyor, birakinca 1.0a donuyor', (tester) async {
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: PressableScale(
          onTap: () {},
          child: const SizedBox(width: 100, height: 100, key: Key('box')),
        ),
      ),
    ));

    AnimatedScale getScale() => tester.widget<AnimatedScale>(find.byType(AnimatedScale));

    expect(getScale().scale, 1.0);

    final gesture = await tester.startGesture(tester.getCenter(find.byKey(const Key('box'))));
    await tester.pump();
    expect(getScale().scale, lessThan(1.0));

    await gesture.up();
    await tester.pump();
    expect(getScale().scale, 1.0);
  });

  testWidgets('onTap cagrilir', (tester) async {
    var tapped = false;
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: PressableScale(
          onTap: () => tapped = true,
          child: const SizedBox(width: 100, height: 100),
        ),
      ),
    ));

    await tester.tap(find.byType(PressableScale));
    expect(tapped, isTrue);
  });

  testWidgets('surukleyip disari cikinca (tap cancel) 1.0a doner', (tester) async {
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: PressableScale(
          onTap: () {},
          child: const SizedBox(width: 100, height: 100, key: Key('box')),
        ),
      ),
    ));

    AnimatedScale getScale() => tester.widget<AnimatedScale>(find.byType(AnimatedScale));

    final gesture = await tester.startGesture(tester.getCenter(find.byKey(const Key('box'))));
    await tester.pump();
    expect(getScale().scale, lessThan(1.0));

    await gesture.moveTo(const Offset(-500, -500));
    await gesture.up();
    await tester.pump();
    expect(getScale().scale, 1.0);
  });
}
