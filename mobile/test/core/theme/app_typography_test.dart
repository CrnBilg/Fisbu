import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:fisbu/core/theme/app_typography.dart';

void main() {
  group('AppTypography', () {
    test('olcek buyukten kucuge dogru kesin/tekil boyutlar tasir', () {
      final sizes = <double>[
        AppTypography.display.fontSize!,
        AppTypography.amountLarge.fontSize!,
        AppTypography.headlineLarge.fontSize!,
        AppTypography.headline.fontSize!,
        AppTypography.title.fontSize!,
        AppTypography.sectionTitle.fontSize!,
        AppTypography.cardTitle.fontSize!,
        AppTypography.body.fontSize!,
        AppTypography.bodySecondary.fontSize!,
        AppTypography.label.fontSize!,
        AppTypography.caption.fontSize!,
        AppTypography.micro.fontSize!,
      ];

      // Kesin olarak azalan bir dizi: her stil bir öncekinden kucuk
      for (var i = 1; i < sizes.length; i++) {
        expect(sizes[i], lessThan(sizes[i - 1]),
            reason: 'index $i (${sizes[i]}) bir onceki (${sizes[i - 1]}) kadar veya daha buyuk olmamali');
      }

      // Tum boyutlar birbirinden farkli (tekil) olmali
      expect(sizes.toSet().length, sizes.length);
    });

    test('copyWith renk uygulanirken diger ozellikleri korur', () {
      final styled = AppTypography.headline.copyWith(color: Colors.red);
      expect(styled.color, Colors.red);
      expect(styled.fontSize, AppTypography.headline.fontSize);
      expect(styled.fontWeight, AppTypography.headline.fontWeight);
    });
  });
}
