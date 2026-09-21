import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:fisbu/core/theme/app_colors.dart';

void main() {
  group('AppColors.cardShadow', () {
    testWidgets('varsayilan tint primary rengiyle standart golge doner', (tester) async {
      late BuildContext capturedContext;
      await tester.pumpWidget(MaterialApp(
        home: Builder(builder: (context) {
          capturedContext = context;
          return const SizedBox();
        }),
      ));

      final shadows = AppColors.cardShadow(capturedContext);

      expect(shadows, hasLength(1));
      expect(shadows.first.blurRadius, 12);
      expect(shadows.first.offset, const Offset(0, 4));
      expect(shadows.first.color, AppColors.primary.withValues(alpha: 0.06));
    });

    testWidgets('ozel tint verildiginde o renge gore golge uretir', (tester) async {
      late BuildContext capturedContext;
      await tester.pumpWidget(MaterialApp(
        home: Builder(builder: (context) {
          capturedContext = context;
          return const SizedBox();
        }),
      ));

      final shadows = AppColors.cardShadow(capturedContext, tint: AppColors.error);

      expect(shadows.first.color, AppColors.error.withValues(alpha: 0.06));
    });
  });
}
